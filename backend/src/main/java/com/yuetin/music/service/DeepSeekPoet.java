package com.yuetin.music.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuetin.music.entity.Poem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * DeepSeek 读诗：把一首诗翻译成 MusicGen 可用的英文音乐描述（prompt）。
 *
 * <p>这是「境界传递」的核心——不再是 12 套固定标签 + 字符级意象字典，
 * 而是让 LLM 真正读诗：结合意境、情绪、叙事起伏写出结构化的作曲说明。
 * 输出 JSON：{"prompt": "...", "mood_en": "...", "rationale": "..."}</p>
 */
@Slf4j
@Component
public class DeepSeekPoet {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

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
            你是一位精通中国古典诗词与传统音乐创作的作曲顾问。
            用户会给你一首古诗（含题目、作者、朝代、情绪标签、诗行）。
            请为它构思一段约 30 秒的中国传统器乐曲，并输出给文本生成音乐模型（MusicGen）的英文提示词。

            要求：
            1. 真正读诗：基于诗的意境、意象、情感层次与叙事起伏来构思，而不是套用标签；
            2. 说明乐器（如 guzheng, guqin, xiao flute, dizi, war drums）、速度、强弱、音色质感；
            3. 描述 30 秒内的结构变化，例如 opens with..., builds into..., ends with...；
            4. 英文输出，45~70 词，一段话；
            5. 严格只输出 JSON，格式：{"prompt": "...", "mood_en": "...", "rationale": "中文一句话说明你的作曲立意"}
            """;

    /** 读诗并返回结构化描述；失败抛异常，由调用方降级为规则标签。 */
    public Result read(Poem poem) {
        String user = String.format(
                "题目：%s%s%n作者：%s（%s）%n情绪标签：%s%n诗行：%n%s",
                poem.getTitle(),
                poem.getSubtitle() == null || poem.getSubtitle().isEmpty() ? "" : "·" + poem.getSubtitle(),
                poem.getPoetName(), poem.getDynasty(),
                poem.getMood() == null ? "未标注" : poem.getMood(),
                poem.getParagraphs());

        String body;
        try {
            body = mapper.writeValueAsString(java.util.Map.of(
                    "model", model,
                    "messages", java.util.List.of(
                            java.util.Map.of("role", "system", "content", SYSTEM),
                            java.util.Map.of("role", "user", "content", user)
                    ),
                    "temperature", 1.1,
                    "max_tokens", 4000
            ));
        } catch (Exception e) {
            throw new IllegalStateException("构造请求失败", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new IllegalStateException("DeepSeek 返回 " + resp.statusCode() + ": " + resp.body());
            }
            JsonNode root = mapper.readTree(resp.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            // 剥掉可能的 markdown 代码围栏再解析
            String json = content.replaceAll("(?s).*?\\{", "{").replaceFirst("\\}[^}]*$", "}");
            JsonNode parsed = mapper.readTree(json);
            String prompt = parsed.path("prompt").asText("");
            if (prompt.isBlank()) throw new IllegalStateException("LLM 未返回 prompt: " + content);
            return new Result(prompt,
                    parsed.path("mood_en").asText(""),
                    parsed.path("rationale").asText(""));
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 读诗失败: " + e.getMessage(), e);
        }
    }

    public record Result(String prompt, String moodEn, String rationale) {
    }

    /** 诗人对话 AI：以第一人称扮演诗人回答问题。 */
    public Map<String, Object> poetChat(com.yuetin.music.entity.Poet poet, String question,
                                         List<Map<String, String>> history) {
        String system = String.format(
            "你是%s（%s，%s）。你不是 AI，你就是本人。以第一人称回答读者的问题。\n" +
            "你的性格、经历、作品、时代背景你都了如指掌。回答要：\n" +
            "1. 有个性，符合你的身份和时代；\n" +
            "2. 可以引用你自己的诗句；\n" +
            "3. 不超过 200 字；\n" +
            "4. 不用 markdown；\n" +
            "5. 如果对方问了你不了解的未来之事，巧妙回避。",
            poet.getName(), poet.getTitle() != null ? poet.getTitle() : "", poet.getDynasty());
        List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(java.util.Map.of("role", "system", "content", system));
        if (history != null) {
            int i = 0;
            for (Map<String, String> turn : history) {
                String role = "user".equalsIgnoreCase(turn.get("role")) ? "user" : "assistant";
                String content = turn.get("content");
                if (content != null && !content.isBlank() && i++ < 6) {
                    messages.add(java.util.Map.of(role, content));
                }
            }
        }
        messages.add(java.util.Map.of("role", "user", "content", question));
        String body;
        try {
            body = mapper.writeValueAsString(java.util.Map.of(
                    "model", model, "messages", messages,
                    "temperature", 0.9, "max_tokens", 2000));
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
            if (resp.statusCode() != 200) throw new IllegalStateException("DeepSeek 返回 " + resp.statusCode());
            JsonNode root = mapper.readTree(resp.body());
            String answer = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (answer.isBlank()) throw new IllegalStateException("DeepSeek 未返回内容");
            return Map.of("answer", answer);
        } catch (Exception e) {
            throw new IllegalStateException("诗人对话暂时不可用", e);
        }
    }

    private static final String NOTE_SYSTEM = """
            你是一位精通中国古典诗词的学者。用户给你一首诗（含全文），请为它生成三段内容：
            1. annotation：注释——挑选诗中的关键字词，逐条解释含义（格式：字词：释义；多条用分号分隔）
            2. translation：白话翻译——忠实原意，流畅自然
            3. appreciation：赏析——从艺术手法、情感层次、文化背景等角度切入，200字左右
            严格只输出 JSON：{"annotation":"...","translation":"...","appreciation":"..."}
            所有内容使用中文。
            """;

    /** 为语料层诗生成注译赏，返回 JSON 三字段。 */
    public Map<String, String> generateNotes(Poem poem) {
        String user = String.format("题目：%s%s\n作者：%s（%s）\n诗行：\n%s",
                poem.getTitle(),
                poem.getSubtitle() == null || poem.getSubtitle().isEmpty() ? "" : "·" + poem.getSubtitle(),
                poem.getPoetName(), poem.getDynasty(),
                poem.getParagraphs());
        String body;
        try {
            body = mapper.writeValueAsString(java.util.Map.of(
                    "model", model,
                    "messages", java.util.List.of(
                            java.util.Map.of("role", "system", "content", NOTE_SYSTEM),
                            java.util.Map.of("role", "user", "content", user)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 3000));
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
                throw new IllegalStateException("DeepSeek 返回 " + resp.statusCode() + ": "
                        + resp.body().substring(0, Math.min(300, resp.body().length())));
            }
            JsonNode root = mapper.readTree(resp.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            String json = content.replaceAll("(?s).*?\\{", "{").replaceFirst("\\}[^}]*$", "}");
            JsonNode parsed = mapper.readTree(json);
            Map<String, String> out = new java.util.HashMap<>();
            out.put("annotation", parsed.path("annotation").asText(""));
            out.put("translation", parsed.path("translation").asText(""));
            out.put("appreciation", parsed.path("appreciation").asText(""));
            if (out.values().stream().allMatch(String::isBlank)) {
                throw new IllegalStateException("DeepSeek 返回内容为空");
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 注译赏生成失败: " + e.getMessage(), e);
        }
    }
}
