package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.entity.AiMusic;
import com.yuetin.music.entity.Poem;
import com.yuetin.music.entity.Song;
import com.yuetin.music.mapper.AiMusicMapper;
import com.yuetin.music.mapper.PoemMapper;
import com.yuetin.music.mapper.SongMapper;
import com.yuetin.music.service.DeepSeekPoet.Result;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 谱曲（古诗转音乐）：
 *  1. 意象→提示词：把诗的情绪/意象映射为音乐描述（tags），诗行原文作为 lyrics；
 *  2. 提供商抽象：配置 AI_ACESTEP_URL 时调用 ACE-Step 真实生成；
 *     未配置（或调用失败且预生成曲存在）时回落 preset 预生成曲，任务流保持完整。
 */
@Slf4j
@Service
public class AiMusicService {

    private final PoemMapper poemMapper;
    private final SongMapper songMapper;
    private final AiMusicMapper aiMusicMapper;
    private final DeepSeekPoet deepSeekPoet;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-music");
        t.setDaemon(true);
        return t;
    });
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Properties aiDurations = new Properties();

    @Value("${yuetin.ai.acestep-url:}")
    private String acestepUrl;

    @Value("${yuetin.audio-dir:./audio}")
    private String audioDir;

    @Value("classpath:ai-durations.properties")
    private org.springframework.core.io.Resource durationsResource;

    public AiMusicService(PoemMapper poemMapper, SongMapper songMapper,
                          AiMusicMapper aiMusicMapper, DeepSeekPoet deepSeekPoet) {
        this.poemMapper = poemMapper;
        this.songMapper = songMapper;
        this.aiMusicMapper = aiMusicMapper;
        this.deepSeekPoet = deepSeekPoet;
    }

    /* ---------------- 意象 → 提示词 ---------------- */

    private static final Map<String, String> MOOD_TAGS = Map.ofEntries(
            Map.entry("月夜", "chinese traditional, guzheng, xiao flute, slow, peaceful, moonlit night"),
            Map.entry("田园", "chinese folk, guzheng, bamboo flute, warm, pastoral, spring"),
            Map.entry("山水", "chinese traditional, guzheng, flowing arpeggio, serene, mountain stream"),
            Map.entry("送别", "chinese traditional, guqin, slow, melancholic, farewell"),
            Map.entry("思乡", "chinese traditional, dizi flute, nostalgic, slow, longing"),
            Map.entry("家国", "chinese traditional, guzheng and drums, solemn, sorrowful, epic"),
            Map.entry("悲秋", "chinese traditional, guqin, sparse notes, autumn, desolate"),
            Map.entry("豪放", "chinese traditional, guzheng, bold, fast glissando, unrestrained"),
            Map.entry("闲适", "chinese folk, guzheng, light, playful, gentle"),
            Map.entry("哲理", "chinese traditional, guqin, meditative, contemplative"),
            Map.entry("壮志", "chinese traditional, war drums, guzheng, heroic, mighty"),
            Map.entry("怀人", "chinese traditional, xiao flute, tender, yearning, quiet")
    );

    /** ACE-Step 风格提示词：情绪标签 + 诗行原文作歌词。 */
    public Map<String, String> buildPrompt(Poem poem) {
        String tags = MOOD_TAGS.getOrDefault(poem.getMood(),
                "chinese traditional, guzheng, peaceful, poetic");
        Map<String, String> prompt = new HashMap<>();
        prompt.put("tags", tags);
        prompt.put("lyrics", "[verse]\n" + poem.getParagraphs());
        prompt.put("durationSec", "60");
        return prompt;
    }

    /* ---------------- 任务编排 ---------------- */

    /** 发起谱曲：有完成的缓存直接返回；进行中的返回原任务；force=true 时无视缓存重新谱。 */
    public AiMusic generate(Long poemId, boolean force) {
        Poem poem = poemMapper.findById(poemId);
        if (poem == null) throw BizException.notFound("诗不存在");

        Claim claim = claimSlot(poem, force);
        if (!claim.fresh()) {
            return view(claim.job());
        }
        AiMusic job = claim.job();

        // 读诗在锁外做：DeepSeek 要十几秒，锁住会挡住其它诗的谱曲请求
        if (deepSeekPoet.configured()) {
            try {
                Result r = deepSeekPoet.read(poem);
                job.setPrompt(r.prompt());
                job.setLyrics("LLM 立意：" + r.rationale());
                aiMusicMapper.update(job);
                log.info("DeepSeek 读诗谱曲: poem={} rationale={}", poem.getId(), r.rationale());
            } catch (Exception e) {
                log.warn("DeepSeek 读诗失败，降级为规则标签: {}", e.getMessage());
            }
        }

        executor.submit(() -> process(job.getId(), poem));
        return view(aiMusicMapper.findById(job.getId()));
    }

    /** 占位结果：job 为本次可用任务，fresh 表示是否由本次请求新建。 */
    private record Claim(AiMusic job, boolean fresh) {}

    /**
     * 抢占建单（唯一的临界区）：已有可用任务就直接复用，否则立刻插入 pending 占位行。
     * 必须先落库再读诗——DeepSeek 读诗要十几秒，若读完才建单，这段时间里到达的
     * 重复请求查不到任何任务，会各自建单，一首诗被谱出两曲。
     */
    private synchronized Claim claimSlot(Poem poem, boolean force) {
        AiMusic latest = aiMusicMapper.latestByPoem(poem.getId());
        if (!force && latest != null && ("done".equals(latest.getStatus())
                || "pending".equals(latest.getStatus()) || "running".equals(latest.getStatus()))) {
            return new Claim(latest, false);
        }

        Map<String, String> prompt = buildPrompt(poem);
        AiMusic job = new AiMusic();
        job.setPoemId(poem.getId());
        job.setProvider(acestepConfigured() ? "acestep" : "preset");
        job.setPrompt(prompt.get("tags"));
        job.setLyrics(prompt.get("lyrics"));
        job.setStatus("pending");
        aiMusicMapper.insert(job);
        return new Claim(job, true);
    }

    /** 任务执行：preset 即时完成；acestep 走 HTTP，失败回落 preset。 */
    private void process(Long jobId, Poem poem) {
        AiMusic job = aiMusicMapper.findById(jobId);
        if (job == null) return;
        job.setStatus("running");
        aiMusicMapper.update(job);

        try {
            Path file = presetFile(poem.getId());
            String provider = job.getProvider();
            Double aiDuration = null;

            if ("acestep".equals(provider) && acestepConfigured()) {
                try {
                    byte[] audio = callAceStep(job);
                    // 文件名带任务 id：重新谱曲时保留旧版本，否则会覆盖上一次的音轨
                    file = Path.of(audioDir).resolve("generated")
                            .resolve("ai-poem-" + poem.getId() + "-" + jobId + ".mp3");
                    Files.createDirectories(file.getParent());
                    Files.write(file, audio);
                    aiDuration = lastAiDuration;   // 边车响应头里的真实时长
                } catch (Exception e) {
                    log.warn("AI 生成失败，回落预生成曲: {}", e.getMessage());
                    provider = "preset";
                    file = presetFile(poem.getId());
                    aiDuration = null;             // 回落时重读预生成时长
                }
            }

            if (file == null || !Files.exists(file)) {
                throw new IllegalStateException("预生成曲不存在，请先运行 tools/synth_poem_ai.py");
            }

            Song track = new Song();
            track.setName("《" + poem.getTitle()
                    + (poem.getSubtitle() != null && !poem.getSubtitle().isEmpty()
                       ? "·" + poem.getSubtitle() : "") + "》AI 谱曲"
                    + versionSuffix(poem.getId()));
            track.setSingerId(3L); // 松涛（合成·古筝）
            track.setPoemId(poem.getId());
            track.setTrackKind("ai");
            track.setUrl("/api/audio/generated/" + file.getFileName());
            double dur = aiDuration != null ? aiDuration : readDuration(poem.getId());
            track.setDuration(java.math.BigDecimal.valueOf(dur));
            track.setStyle(poem.getMood());
            track.setPlayCount(0L);
            songMapper.insert(track);

            job.setProvider(provider);
            job.setTrackId(track.getId());
            job.setStatus("done");
            job.setFinishTime(new Date());
            aiMusicMapper.update(job);
            log.info("AI 谱曲完成: poem={} provider={} track={}", poem.getId(), provider, track.getId());
        } catch (Exception e) {
            log.error("AI 谱曲失败: poem={}", poem.getId(), e);
            job.setStatus("failed");
            job.setError(e.getMessage() == null ? "生成失败" : e.getMessage().substring(0, Math.min(490, e.getMessage().length())));
            job.setFinishTime(new Date());
            aiMusicMapper.update(job);
        }
    }

    /** 任务状态查询（前端轮询）。 */
    public AiMusic job(Long id) {
        AiMusic job = aiMusicMapper.findById(id);
        if (job == null) throw BizException.notFound("任务不存在");
        return view(job);
    }

    /** 某诗已完成的 AI 曲列表。 */
    public List<AiMusic> listDone(Long poemId) {
        return aiMusicMapper.findByPoemWithTrack(poemId);
    }

    private AiMusic view(AiMusic job) {
        if ("done".equals(job.getStatus()) && job.getTrackId() != null) {
            Song s = songMapper.findById(job.getTrackId());
            if (s != null) {
                job.setAudioUrl(s.getUrl());
                job.setTrackName(s.getName());
                job.setDuration(s.getDuration() == null ? null : s.getDuration().doubleValue());
            }
        }
        return job;
    }

    private boolean acestepConfigured() {
        return acestepUrl != null && !acestepUrl.isBlank();
    }

    private Path presetFile(Long poemId) {
        return Path.of(audioDir).resolve("generated").resolve("ai-poem-" + poemId + ".mp3");
    }

    /** 重新谱曲时给新版本编号，避免同名音轨分不清：第二版起为「·其二」。 */
    private String versionSuffix(Long poemId) {
        int n = songMapper.countAiByPoem(poemId);
        if (n <= 0) return "";
        String[] cn = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        return n <= cn.length ? "·其" + cn[n] : "·第" + (n + 1) + "版";
    }

    /**
     * 调用自部署 ACE-Step 推理服务。
     * 约定接口：POST {url} body {tags, lyrics, durationSec} → 返回 mp3 二进制；
     * 若你的部署返回 JSON（如 {audio: "<url>"}），在后续迭代中按实际返回适配。
     */
    private Double lastAiDuration;

    private byte[] callAceStep(AiMusic job) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(acestepUrl))
                .timeout(Duration.ofMinutes(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"tags\":\"" + escapeJson(job.getPrompt())
                        + "\",\"lyrics\":\"\",\"prompt\":\"" + escapeJson(job.getPrompt())
                        + "\",\"durationSec\":60}"))
                .build();
        HttpResponse<byte[]> resp = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            throw new IOException("ACE-Step 返回 " + resp.statusCode());
        }
        lastAiDuration = resp.headers().firstValue("X-Duration-Seconds")
                .map(Double::parseDouble).orElse(null);
        return resp.body();
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "").replace("\n", "\\n");
    }

    private Double readDuration(Long poemId) {
        try {
            if (aiDurations.isEmpty()) {
                try (InputStream in = durationsResource.getInputStream()) {
                    aiDurations.load(in);
                }
            }
            String v = aiDurations.getProperty(String.valueOf(poemId));
            return v == null ? 60.0 : Double.parseDouble(v);
        } catch (IOException e) {
            return 60.0;
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
