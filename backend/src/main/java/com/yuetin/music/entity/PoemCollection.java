package com.yuetin.music.entity;

import lombok.Data;

import java.util.Date;

/** 主题选集：策展式组诗。 */
@Data
public class PoemCollection {
    private Long id;
    private String title;
    private String subtitle;
    private String intro;
    private String mood;
    private Integer sort;
    private Date createTime;

    /** 关联统计：选集内诗数 */
    private Integer poemCount;
}
