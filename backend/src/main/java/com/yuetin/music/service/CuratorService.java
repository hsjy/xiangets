package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.entity.PoemCollection;
import com.yuetin.music.entity.Poem;
import com.yuetin.music.mapper.CollectionMapper;
import com.yuetin.music.mapper.PoetMapper;
import com.yuetin.music.mapper.PoetRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 策展内容：主题选集与诗人关系图谱。 */
@Service
@RequiredArgsConstructor
public class CuratorService {

    private final CollectionMapper collectionMapper;
    private final PoetMapper poetMapper;
    private final PoetRelationMapper poetRelationMapper;

    public List<PoemCollection> listCollections() {
        return collectionMapper.list();
    }

    public Map<String, Object> collectionDetail(Long id) {
        PoemCollection c = collectionMapper.findById(id);
        if (c == null) throw BizException.notFound("选集不存在");
        List<Poem> poems = collectionMapper.findPoems(id);
        Map<String, Object> data = new HashMap<>();
        data.put("collection", c);
        data.put("poems", poems);
        return data;
    }

    public List<Map<String, Object>> poetRelations(Long poetId) {
        if (poetMapper.findById(poetId) == null) throw BizException.notFound("诗人不存在");
        return poetRelationMapper.findByPoet(poetId);
    }
}
