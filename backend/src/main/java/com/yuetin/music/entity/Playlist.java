package com.yuetin.music.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Playlist {
    private Long id;
    private String name;
    private String intro;
    private String cover;
    private String style;
    /** NULL 表示官方歌单。 */
    private Long creatorId;
    /** 对应数据库列 is_public（public 是 MySQL 保留字）。 */
    private Integer isPublic;
    private Date createTime;

    /** 关联查询带出，供歌单卡片展示。 */
    private String creatorName;
    private String creatorAvatar;
    private Integer songCount;
}
