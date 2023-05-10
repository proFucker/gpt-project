package com.xfd.wChat.practise;

//目前的聊天状态
public enum ChatStatus {

    //运势预测相关的进度
    JUST_ENTER("刚刚进入公众号"),
    SELECTING_MENU("选择菜单中"),
    SELECTING_DESTINY("选择预测方向中"),
    DESCRIBING_SELF("描述近况中..."),
    SELECTING_PRACTISE_TIME("输入要预测的时间"),
    REPLENISH_SELF_DETAIL("补充个人信息"),
    PREDICTING("预测中..."),
    PREDICT_SUCCESS("预测成功(可能还未推送或者推送失败)"),
    PREDICT_FAIL("推送预测消息失败"),
    ;

    ChatStatus(String describe) {
        this.describe = describe;
    }

    private String describe;


}
