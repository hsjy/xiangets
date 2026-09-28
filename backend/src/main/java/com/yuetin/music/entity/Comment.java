package com.yuetin.music.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Comment {
    private Long id;
    private Long songId;
    private Long userId;
    private String content;
    private Date createTime;

    private String username;
    private String avatar;
}
