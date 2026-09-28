package com.yuetin.music.mapper;

import com.yuetin.music.entity.Playlist;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PlaylistMapper {

    List<Playlist> search(@Param("keyword") String keyword, @Param("style") String style);

    /** 当前用户创建的歌单。 */
    List<Playlist> findByCreator(@Param("creatorId") Long creatorId);

    /** 当前用户收藏的歌单。 */
    List<Playlist> findFavorited(@Param("userId") Long userId);

    Playlist findById(@Param("id") Long id);

    int insert(Playlist playlist);

    int update(Playlist playlist);

    int deleteById(@Param("id") Long id);

    /** 歌单收藏 / 取消收藏，返回受影响行数供上层判断状态。 */
    int addFavorite(@Param("userId") Long userId, @Param("playlistId") Long playlistId);

    int removeFavorite(@Param("userId") Long userId, @Param("playlistId") Long playlistId);

    boolean isFavorited(@Param("userId") Long userId, @Param("playlistId") Long playlistId);

    long countAll();
}
