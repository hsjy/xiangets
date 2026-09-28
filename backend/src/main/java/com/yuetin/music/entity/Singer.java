package com.yuetin.music.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Singer {
    private Long id;
    private String name;
    private Integer gender;
    private String avatar;
    private String style;
    private String location;
    private String birth;
    private String intro;
    private Integer songCount;
    private Date createTime;
}
