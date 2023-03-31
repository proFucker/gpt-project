package com.xfd.common;

import lombok.Getter;
import lombok.Setter;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import org.springframework.context.ApplicationContext;

@Getter
@Setter
@SuppressWarnings("unchecked")
public class WChatContext {
    private static ThreadLocal<WChatContext> contextHolder = new ThreadLocal<>();

    private String user;
    private Long userId;

    //标志是哪个公众号
    private String witchWChatService;

    private ApplicationContext applicationContext;

    //wx转发的消息
    private WxMpXmlMessage wxMpXmlMessage;

    public static void setDesigned(WChatContext designed) {
        contextHolder.set(designed);
    }

    public static void setNew() {
        contextHolder.set(new WChatContext());
    }

    public static WChatContext get() {
        return contextHolder.get();
    }

    public static String getWChatUser() {
        return contextHolder.get().getUser();
    }

    public static String getInputContent() {
        return contextHolder.get().getWxMpXmlMessage().getContent();
    }

    public static void clear() {
        contextHolder.remove();
    }

    public static <T> T getBean(String beanName) {
        return (T) contextHolder.get().getApplicationContext().getBean(beanName);
    }

}
