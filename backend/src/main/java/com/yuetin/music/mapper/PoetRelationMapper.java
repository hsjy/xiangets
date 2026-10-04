package com.yuetin.music.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface PoetRelationMapper {

    /** 某诗人的关系（含对方诗人信息） */
    List<Map<String, Object>> findByPoet(@Param("poetId") Long poetId);
}
