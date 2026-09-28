package com.yuetin.music.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SongLikeMapper {

    /** 返回该用户喜欢的歌曲 ID，按喜欢时间倒序。 */
    List<Long> findSongIds(@Param("userId") Long userId);

    int add(@Param("userId") Long userId, @Param("songId") Long songId);

    int remove(@Param("userId") Long userId, @Param("songId") Long songId);

    boolean exists(@Param("userId") Long userId, @Param("songId") Long songId);
}
