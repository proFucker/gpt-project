package com.xfd.common.dao;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
//用户通用信息
public class UserCommonInfo {

    private String name;
    private String birthday;
    private String heightWeight;
    private String job;

    private Long updateTime;
//    private String character;
//    private String recentInfo;
}
