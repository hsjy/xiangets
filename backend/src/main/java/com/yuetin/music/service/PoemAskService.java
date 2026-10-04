package com.yuetin.music.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuetin.music.common.BizException;
import com.yuetin.music.entity.Poem;
import com.yuetin.music.mapper.PoemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 问诗：DeepSeek 带诗境上下文的问答。
 * 系统提示注入诗全文与注译赏，支持多轮（前端回传最近对话）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoemAskService {

    private final PoemMapper poemMapper;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${yuetin.ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${yuetin.ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${yuetin.ai.deepseek.model:deepseek-v4-flash}")
    private String model;

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private static final String SYSTEM = """
            你是一位学识渊博而温雅的中国古典诗词学者，正在与一位读诗的人对话。
            下面是你们正在共读的一首诗，以及它的注释、译文与赏析，供你随时取用。
            回答要求：
            1. 紧扣这首诗与提问，可以延展到诗人生平、时代背景、相关名篇，但不离题万里；
            2. 温雅、简净、有见地，像一位师长随口谈艺，不堆砌辞藻，不用套话开头；
            3. 一般不超过 200 字；对方若明确要求展开，可以写到 400 字；
            4. 不使用 markdown 标记，直接以自然段回话。
            """;

    /** 问答主入口。 */
    public Map<String, Object> ask(Long poemId, String question, List<Map<String, String>> history) {
        if (question == null || question.isBlank()) throw BizException.badRequest("问题不能为空");
        Poem poem = poemMapper.findById(poemId);
        if (poem == null) throw BizException.notFound("诗不存在");
        if (!configured()) throw BizException.badRequest("未配置 DeepSeek API Key");

        String context = String.format(
                "《%s%s》 %s %s（%s）%n诗行：%n%s%n%s",
                poem.getTitle(),
                poem.getSubtitle() == null || poem.getSubtitle().isEmpty() ? "" : "·" + poem.getSubtitle(),
                poem.getGenre() == null ? "" : poem.getGenre(),
                poem.getPoetName(), poem.getDynasty(),
                poem.getParagraphs(),
                buildAux(poem));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM + "\n\n【共读的诗】\n" + context));
        if (history != null) {
            int i = 0;
            for (Map<String, String> turn : history) {
                String role = "user".equalsIgnoreCase(turn.get("role")) ? "user" : "assistant";
                String content = turn.get("content");
                if (content != null && !content.isBlank() && i++ < 6) {
                    messages.add(Map.of(role, content));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", question));

        String body;
        try {
            body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", messages,
                    "temperature", 0.8,
                    "max_tokens", 2000));
        } catch (Exception e) {
            throw new IllegalStateException("构造请求失败", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new IllegalStateException("DeepSeek 返回 " + resp.statusCode());
            }
            JsonNode root = mapper.readTree(resp.body());
            String answer = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (answer.isBlank()) throw new IllegalStateException("DeepSeek 未返回内容");
            return Map.of("answer", answer);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("问诗失败: {}", e.getMessage());
            throw new IllegalStateException("问诗暂时不可用，请稍后再试", e);
        }
    }

    private String buildAux(Poem poem) {
        StringBuilder sb = new StringBuilder();
        if (poem.getAnnotation() != null && !poem.getAnnotation().isBlank()) sb.append("注释：").append(poem.getAnnotation()).append('\n');
        if (poem.getTranslation() != null && !poem.getTranslation().isBlank()) sb.append("译文：").append(poem.getTranslation()).append('\n');
        if (poem.getAppreciation() != null && !poem.getAppreciation().isBlank()) sb.append("赏析：").append(poem.getAppreciation()).append('\n');
        return sb.toString();
    }
}
