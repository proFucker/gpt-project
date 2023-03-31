package com.xfd.openai.service;

//目前的聊天状态
public enum ChatStatus {

    JUST_ENTER,//刚刚进入
    SELECTING_MENU,

    //运势预测相关的进度
    SELECTING_DESTINY,
    DESCRIBING_SELF,
    SELECTING_TIME,
    REPLENISH_SELF_DETAIL,
    PREDICTING,
    ;
}
