package com.xfd.common.dao;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class ChatRecord {
    private String userId;
    private String role;
    private String content;
    private String contextKey;
}
