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
}
