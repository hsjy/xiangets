package com.yuetin.music.entity;

import lombok.Data;

import java.util.Date;

/**
 * 诗：欣赏主体。诗行以换行分隔存储，译/注/赏随诗。
 */
@Data
public class Poem {
    private Long id;
    private String title;
    private String subtitle;
    private Long poetId;
    private String dynasty;
    private String genre;
    /** 诗行，换行分隔 */
    private String paragraphs;
    private String annotation;
    private String translation;
    private String appreciation;
    /** 情绪意象：月夜/田园/送别/边塞/思乡/壮志/闲适/怀人/哲理/豪放 */
    private String mood;
    private String famousLine;

    /** 内容分层：1 精选（带注释译文赏析）/ 2 语料（批量导入全文） */
    private Integer tier;
    private Date createTime;

    /** 关联带出 */
    private String poetName;
    private String poetYears;
    private String poetTitle;
}
