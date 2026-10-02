package com.yuetin.music.mapper;

import com.yuetin.music.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentMapper {

    List<Comment> findBySong(@Param("songId") Long songId);

    /** 诗的感悟，倒序，带评论者与诗题。 */
    List<Comment> findByPoem(@Param("poemId") Long poemId);

    /** 我的感悟（书斋）。 */
    List<Comment> mine(@Param("userId") Long userId);

    int insert(Comment comment);

    int insertPoem(Comment comment);

    /** 仅允许删除自己的评论/感悟，返回受影响行数供上层判断权限。 */
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
