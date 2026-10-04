package com.yuetin.music.mapper;

import com.yuetin.music.entity.PoemCollection;
import com.yuetin.music.entity.Poem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CollectionMapper {

    /** 选集列表（带诗数，按 sort 排序） */
    List<PoemCollection> list();

    PoemCollection findById(@Param("id") Long id);

    /** 选集内诗（按策展顺序） */
    List<Poem> findPoems(@Param("id") Long id);
}
