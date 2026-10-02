package com.yuetin.music.mapper;

import com.yuetin.music.entity.Song;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface SongMapper {

    /** 列表查询，全部筛选条件可空；join 出 singerName 供前端直接展示。 */
    List<Song> search(@Param("keyword") String keyword,
                      @Param("style") String style,
                      @Param("singerId") Long singerId,
                      @Param("poemId") Long poemId,
                      @Param("sort") String sort);

    Song findById(@Param("id") Long id);

    /** 按播放量倒序取热门歌曲。 */
    List<Song> findHot(@Param("limit") int limit);

    /** 按给定顺序返回歌单内的歌曲（配合 playlist_song.sort_no）。 */
    List<Song> findByIdsInOrder(@Param("ids") List<Long> ids);

    /** 某诗已有的 AI 曲数量（用于给新版本编号，如「其二」）。 */
    int countAiByPoem(@Param("poemId") Long poemId);

    int insert(Song song);

    int update(Song song);

    int deleteById(@Param("id") Long id);

    /** 播放量 +1，返回自增后的值供前端即时展示。 */
    int increasePlayCount(@Param("id") Long id);

    long countAll();

    long sumPlayCount();
}
