package com.yuetin.music.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PlaylistSongMapper {

    /** 返回该歌单的歌曲 ID，已按 sort_no 升序。 */
    List<Long> findSongIds(@Param("playlistId") Long playlistId);

    /** 加入歌曲；重复加入由 INSERT IGNORE 静默跳过。sortNo 由 Service 层按当前最大值 +1 计算。 */
    int add(@Param("playlistId") Long playlistId, @Param("songId") Long songId, @Param("sortNo") int sortNo);

    int remove(@Param("playlistId") Long playlistId, @Param("songId") Long songId);

    /** 重排前清空该歌单的全部关联行。 */
    int deleteByPlaylist(@Param("playlistId") Long playlistId);

    /** 整表重排：按传入顺序写入 sort_no。 */
    int reorder(@Param("playlistId") Long playlistId, @Param("songIds") List<Long> songIds);

    int countByPlaylist(@Param("playlistId") Long playlistId);

    /** 当前最大 sort_no，空歌单返回 0。 */
    int maxSortNo(@Param("playlistId") Long playlistId);
}
