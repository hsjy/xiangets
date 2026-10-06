package com.yuetin.music.mapper;

import com.yuetin.music.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CommentMapper {

    List<Comment> findBySong(@Param("songId") Long songId);

    /** 诗的感悟，倒序，带评论者与诗题。 */
    List<Comment> findByPoem(@Param("poemId") Long poemId, @Param("viewerId") Long viewerId);

    /** 我的感悟（书斋）。 */
    List<Comment> mine(@Param("userId") Long userId);

    int insert(Comment comment);

    int insertPoem(Comment comment);

    /** 仅允许删除自己的评论/感悟，返回受影响行数供上层判断权限。 */
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int deleteById(@Param("id") Long id);

    /** 全站感悟（管理后台）：带用户名与诗题 */
    List<Map<String, Object>> findAllWithMeta();

    /** 某感悟的点赞数 */
    int likeCount(@Param("commentId") Long commentId);

    /** 当前用户是否已点赞 */
    int likedBy(@Param("commentId") Long commentId, @Param("userId") Long userId);

    int insertLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    int deleteLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /** 回复对象（取其作者名与根 id，用于「回复 @xx」与一级扁平化） */
    Map<String, Object> findMeta(@Param("id") Long id);

    /** 删除某感悟的回复（删主感悟时级联） */
    int deleteReplies(@Param("parentId") Long parentId);

    /** 诗感悟单条（删前校验归属） */
    Comment findByIdPoem(@Param("id") Long id);

    int deleteByIdPoem(@Param("id") Long id);

    /** 级联清理点赞：本条及其回复 */
    int deleteLikesCascade(@Param("id") Long id);
}