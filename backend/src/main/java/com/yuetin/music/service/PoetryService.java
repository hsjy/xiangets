package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.entity.Poem;
import com.yuetin.music.entity.Poet;
import com.yuetin.music.mapper.PoemMapper;
import com.yuetin.music.mapper.PoetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PoetryService {

    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;

    /** 诗选：朝代/情绪/关键词自由组合。 */
    public List<Poem> listPoems(String dynasty, String mood, String keyword) {
        return poemMapper.search(blank(dynasty), blank(mood), blank(keyword));
    }

    public Poem poemDetail(Long id) {
        Poem poem = poemMapper.findById(id);
        if (poem == null) throw BizException.notFound("诗不存在");
        return poem;
    }

    /**
     * 今日一诗：按年内天数确定性轮换，同一天所有人看到同一首。
     */
    public Map<String, Object> todayPoem() {
        long count = poemMapper.countAll();
        Map<String, Object> data = new HashMap<>();
        if (count == 0) {
            data.put("poem", null);
            return data;
        }
        int day = LocalDate.now().getDayOfYear();
        Poem poem = poemMapper.findByOffset(day % count);
        data.put("poem", poem);
        data.put("date", LocalDate.now().toString());
        data.put("index", day % count);
        data.put("total", count);
        return data;
    }

    /** 诗人名录（含各人存诗数）。 */
    public List<Poet> listPoets(String dynasty) {
        return poetMapper.list(blank(dynasty));
    }

    /** 诗人页：生平故事 + 存诗。 */
    public Map<String, Object> poetDetail(Long id) {
        Poet poet = poetMapper.findById(id);
        if (poet == null) throw BizException.notFound("诗人不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("poet", poet);
        data.put("poems", poemMapper.findByPoet(id));
        return data;
    }

    /** 情绪/朝代筛选的可选项（前端筛选项不写死）。 */
    public Map<String, Object> facets() {
        Map<String, Object> data = new HashMap<>();
        data.put("dynasties", poetMapper.list(null).stream()
                .map(Poet::getDynasty).distinct().sorted().toList());
        data.put("moods", poemMapper.search(null, null, null).stream()
                .map(Poem::getMood).filter(m -> m != null && !m.isBlank())
                .distinct().sorted().toList());
        data.put("poemCount", poemMapper.countAll());
        return data;
    }

    private String blank(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
