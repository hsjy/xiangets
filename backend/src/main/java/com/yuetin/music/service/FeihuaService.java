package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.mapper.PoemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞花令：在语料库中随机抽取含令字的诗句。
 * 两步法（先计数再随机偏移）避免 ORDER BY RAND() 全表排序。
 */
@Service
@RequiredArgsConstructor
public class FeihuaService {

    private final PoemMapper poemMapper;
    private final com.yuetin.music.mapper.UserMapper userMapper;

    public Map<String, Object> draw(String ch, List<String> exclude) {
        if (ch == null || ch.isBlank() || ch.length() != 1) {
            throw BizException.badRequest("令字须为单个汉字");
        }
        long total = poemMapper.countLinesWithChar(ch);
        if (total == 0) throw BizException.badRequest("诗库中未找到含「" + ch + "」的诗句");
        // 随机取若干条，跳过已用句
        for (int attempt = 0; attempt < 6; attempt++) {
            long offset = (long) (Math.random() * total);
            Map<String, Object> line = poemMapper.findLineWithChar(ch, offset);
            if (line == null) continue;
            String text = String.valueOf(line.get("line"));
            if (exclude != null && exclude.stream().anyMatch(e -> norm(e).equals(norm(text)))) continue;
            Map<String, Object> out = new HashMap<>(line);
            out.put("line", text);
            return out;
        }
        throw BizException.badRequest("含「" + ch + "」的诗句已被用尽");
    }

    /** 校验用户对句：含令字、库中可查、未被用过。 */
    public Map<String, Object> check(String ch, String line, List<String> exclude) {
        if (line == null || line.isBlank()) throw BizException.badRequest("请写下你的诗句");
        String n = norm(line);
        if (ch != null && !ch.isBlank() && !n.contains(ch)) {
            throw BizException.badRequest("对句里没有「" + ch + "」字");
        }
        if (n.length() < 4) throw BizException.badRequest("太短了，至少写完整一句");
        if (exclude != null && exclude.stream().anyMatch(e -> norm(e).equals(n))) {
            throw BizException.badRequest("这句已经用过了");
        }
        long hit = poemMapper.countLineExists(n);
        if (hit == 0) throw BizException.badRequest("诗库里没有找到这一句，再想想？");
        // 每对上一句 +10 分（匿名游玩不计分）
        Long uid = com.yuetin.music.common.UserContext.id();
        if (uid != null) userMapper.addPoints(uid, 10);
        return Map.of("ok", true);
    }

    /** 诗词接龙：校验诗句是否以指定字开头且在库中存在 */
    public Map<String, Object> chain(String startChar, String line, List<String> exclude) {
        if (startChar == null || startChar.length() != 1) throw BizException.badRequest("起始字须为单个汉字");
        if (line == null || line.trim().length() < 4) throw BizException.badRequest("诗句太短");
        String n = norm(line);
        if (!n.startsWith(startChar)) throw BizException.badRequest("诗句必须以「" + startChar + "」开头");
        if (exclude != null && exclude.stream().anyMatch(e -> norm(e).equals(n))) {
            throw BizException.badRequest("这句已经用过了");
        }
        long hit = poemMapper.countLineExists(n);
        if (hit == 0) throw BizException.badRequest("诗库里没有找到这一句，再想想？");
        Long uid = com.yuetin.music.common.UserContext.id();
        if (uid != null) userMapper.addPoints(uid, 10);
        return Map.of("ok", true);
    }

    private String norm(String s) {
        return s == null ? "" : s.replaceAll("[，。？！；、：''\"\"\\s]", "");
    }
}
