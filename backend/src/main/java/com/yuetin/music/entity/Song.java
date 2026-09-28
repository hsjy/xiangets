package com.yuetin.music.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Song {
    private Long id;
    private String name;
    private Long singerId;
    private String cover;
    private String url;
    /** 时长（秒）。用 BigDecimal 以保留音频探测得到的小数秒。 */
    private BigDecimal duration;
    private String style;
    private Long playCount;
    private Date createTime;

    /** 关联查询带出的歌手名，前端表格直接展示。 */
    private String singerName;
}
