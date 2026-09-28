package com.yuetin.music.mapper;

import com.yuetin.music.entity.Singer;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SingerMapper {

    Singer findById(@Param("id") Long id);

    /** keyword 同时匹配歌手名、风格与地区。 */
    List<Singer> search(@Param("keyword") String keyword);

    int insert(Singer singer);

    int update(Singer singer);

    int deleteById(@Param("id") Long id);

    long countAll();
}
