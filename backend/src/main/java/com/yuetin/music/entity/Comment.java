package com.yuetin.music.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Comment {
    private Long id;
    private Long songId;
    /** 关联的诗；NULL=旧曲评论 */
    private Long poemId;
    private Long userId;
    private String content;
    private Date createTime;

    private String username;
    private String avatar;
    /** 感悟查询时带出诗题 */
    private String poemTitle;

    /** 互动：父感悟（NULL=主感悟）；展示用点赞数与是否已赞 */
    private Long parentId;
    private Integer likeCount;
    private Boolean likedByMe;
    /** 回复对象的用户名（回复某条感悟时展示「回复 @xx」） */
    private String replyTo;
}
