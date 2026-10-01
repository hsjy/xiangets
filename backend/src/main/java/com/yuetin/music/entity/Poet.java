package com.yuetin.music.entity;

import lombok.Data;

import java.util.Date;

/**
 * 诗人：字/号/朝代/生平，以及作为一级内容的「诗人故事」。
 */
@Data
public class Poet {
    private Long id;
    private String name;
    private String zi;
    private String hao;
    private String dynasty;
    private String years;
    private String title;
    private String intro;
    private String story;
    private String storyTitle;
    private Date createTime;

    /** 关联统计：存诗数量（列表页展示）。 */
    private Integer poemCount;
}
