package com.yuetin.music.mapper;

import com.yuetin.music.entity.AiMusic;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiMusicMapper {

    AiMusic findById(@Param("id") Long id);

    /** 某诗最近一次任务（缓存判断用） */
    AiMusic latestByPoem(@Param("poemId") Long poemId);

    int insert(AiMusic job);

    int update(AiMusic job);

    /** 任务完成后回填音轨信息，便于前端直接播放 */
    List<AiMusic> findByPoemWithTrack(@Param("poemId") Long poemId);

    /** 删除指向某音轨的谱曲任务（音轨删除时保证可重新生成） */
    int deleteByTrackId(@Param("trackId") Long trackId);
}