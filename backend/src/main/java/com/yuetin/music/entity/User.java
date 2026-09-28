package com.yuetin.music.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class User {
    private Long id;
    private String username;

    /** 只写：响应中永远不输出密码字段。 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String name;
    private String avatar;
    private String email;
    private String phone;
    private Integer sex;
    private String birth;
    private String intro;
    private String role;
    private Date createTime;
}
