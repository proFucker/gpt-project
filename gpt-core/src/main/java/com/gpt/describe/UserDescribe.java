package com.gpt.describe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDescribe {
    private String name;
    private String birthday;
    private String nature;
    private String weightHeight;
    private String job;
    //用户当下的困难
    private String problem;
    //用户想要占卜的方向
    private String topic;
}
