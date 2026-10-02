package com.yuetin.music.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface PoemFavoriteMapper {

    int add(@Param("userId") Long userId, @Param("poemId") Long poemId);

    int remove(@Param("userId") Long userId, @Param("poemId") Long poemId);

    boolean exists(@Param("userId") Long userId, @Param("poemId") Long poemId);

    /** 书斋：收藏的诗（含诗人/名句），按收藏时间倒序。 */
    List<Map<String, Object>> listByUser(@Param("userId") Long userId);
}
