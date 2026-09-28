package com.yuetin.music.mapper;

import com.yuetin.music.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentMapper {

    List<Comment> findBySong(@Param("songId") Long songId);

    int insert(Comment comment);

    /** 仅允许删除自己的评论，返回受影响行数供上层判断权限。 */
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
