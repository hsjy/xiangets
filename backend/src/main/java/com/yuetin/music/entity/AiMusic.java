package com.yuetin.music.entity;

import lombok.Data;

import java.util.Date;

/** AI 谱曲任务：一次「古诗转音乐」的完整记录。 */
@Data
public class AiMusic {
    private Long id;
    private Long poemId;
    /** preset 预生成 / acestep 真实生成 */
    private String provider;
    private String prompt;
    private String lyrics;
    /** pending / running / done / failed */
    private String status;
    private String error;
    private Long trackId;
    private Date createTime;
    private Date finishTime;

    /** 关联带出：完成后可直接播放的音轨信息 */
    private String audioUrl;
    private String trackName;
    private Double duration;
    private String poemTitle;
}
