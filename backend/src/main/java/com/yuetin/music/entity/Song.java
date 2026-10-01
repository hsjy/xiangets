package com.yuetin.music.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Song {
    private Long id;
    /** 关联的诗；NULL=纯氛围曲目 */
    private Long poemId;
    /** recitation 朗诵配乐 / bgm 纯音乐 / ai 生成 */
    private String trackKind;
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
