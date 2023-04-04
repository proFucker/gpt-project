package com.xfd.wChat.service;

import com.google.common.cache.Cache;
import com.xfd.openai.service.ChatStatus;
import com.xfd.common.WChatContext;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.util.http.okhttp.DefaultOkHttpClientBuilder;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceOkHttpImpl;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.config.impl.WxMpMapConfigImpl;
import me.chanjar.weixin.mp.util.xml.XStreamTransformer;
import okhttp3.ConnectionPool;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
@Slf4j
public class WChatService {

    @Value("${wChat.appID}")
    private String appID;
    @Value("${wChat.appSecret}")
    private String appSecret;


//    @Bean
//    public WxMpService configWChatService() {
//        WxMpServiceOkHttpImpl wxMpServiceOkHttp = new WxMpServiceOkHttpImpl();
//        wxMpServiceOkHttp.setMaxRetryTimes(1);
//        WxMpMapConfigImpl wxMpConfigStorage = new WxMpMapConfigImpl();
//        wxMpConfigStorage.setMaxRetryTimes(1);
//        wxMpConfigStorage.setAppId(appID);
//        wxMpConfigStorage.setSecret(appSecret);
//        configOkHttpClient();
////        wxMpConfigStorage.setHttpProxyHost();
//        wxMpServiceOkHttp.setWxMpConfigStorage(wxMpConfigStorage);
//        wxMpServiceOkHttp.initHttp();
//        return wxMpServiceOkHttp;
//    }

    private WxMpService wxMpService;

    @PostConstruct
    private void initInnerService() {
        WxMpServiceOkHttpImpl wxMpServiceOkHttp = new WxMpServiceOkHttpImpl();
        wxMpServiceOkHttp.setMaxRetryTimes(1);
        WxMpMapConfigImpl wxMpConfigStorage = new WxMpMapConfigImpl();
        wxMpConfigStorage.setMaxRetryTimes(1);
        wxMpConfigStorage.setAppId(appID);
        wxMpConfigStorage.setSecret(appSecret);
        configOkHttpClient();
//        wxMpConfigStorage.setHttpProxyHost();
        wxMpServiceOkHttp.setWxMpConfigStorage(wxMpConfigStorage);
        wxMpServiceOkHttp.initHttp();
        wxMpService = wxMpServiceOkHttp;
    }


    @Autowired
    @Qualifier("wChat_status_cache")
    private Cache<String, ChatStatus> wChatCache;

    @Autowired
    @Qualifier("wChat_data")
    private Cache<String, Object> wChatDataCache;

    public String getWxAccessToken() {
        try {
            return wxMpService.getAccessToken();
        } catch (WxErrorException e) {
            try {
                return wxMpService.getAccessToken(true);
            } catch (WxErrorException gg) {
                return null;
            }
        }
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    private void refreshWxAccessToken() {
        try {
            wxMpService.getAccessToken(true);
        } catch (WxErrorException e) {
            log.error("wx_access_token error", e);
        }
    }

    private void configOkHttpClient() {
        DefaultOkHttpClientBuilder.get()
            .connectTimeout(5000L, TimeUnit.MILLISECONDS)
            .readTimeout(5000L, TimeUnit.MILLISECONDS)
            .writeTimeout(5000L, TimeUnit.MILLISECONDS)
            .callTimeout(100000L, TimeUnit.MILLISECONDS)
            .setConnectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS));

    }

    public String processWXPushData(String xmlData) {
        WxMpXmlMessage wxMpXmlMessage = XStreamTransformer.fromXml(WxMpXmlMessage.class, xmlData);
        WChatContext.get().setWxMpXmlMessage(wxMpXmlMessage);
        WChatContext.get().setUser(wxMpXmlMessage.getFromUser());
        WxMpXmlMessage returnMsg = null;
        switch (wxMpXmlMessage.getMsgType()) {
            case WxConsts.XmlMsgType.TEXT:
                returnMsg = processTextMsg();
                break;
            case WxConsts.XmlMsgType.IMAGE:
                returnMsg = boxTextWxReturnMessage("这图好好看");
                break;
            case WxConsts.XmlMsgType.VOICE:
                returnMsg = boxTextWxReturnMessage("小优听不懂,只看得懂文字哦");
                break;
            default:
                returnMsg = boxTextWxReturnMessage("小优还不够智能,请再次输入");
                break;
        }
        return XStreamTransformer.toXml(WxMpXmlMessage.class, returnMsg);

    }

    private WxMpXmlMessage processTextMsg() {
        ChatStatus chatStatus = getUserWChatStatus();
        WxMpXmlMessage rtn = null;
        switch (chatStatus) {
            case JUST_ENTER:
                if (StringUtils.equals(WChatContext.getInputContent().trim(), "占卜")) {
                    rtn = boxTextWxReturnMessage("知道辽,想预测哪方面的运势捏?\n事业\n爱情\n友情\n财运\n其他\n随便");
                    updateUserWChatStatus(ChatStatus.SELECTING_DESTINY);
                } else {
                    rtn = boxTextWxReturnMessage("这里是小优,有什么可以帮您?\n" +
                        "预测运势请输入\"占卜\"");
                    updateUserWChatStatus(ChatStatus.SELECTING_MENU);
                }
                break;
            case SELECTING_MENU:
                if (StringUtils.equals(WChatContext.getInputContent().trim(), "占卜")) {
                    rtn = boxTextWxReturnMessage("知道辽,想预测哪方面的运势捏?\n事业\n爱情\n友情\n财运\n其他\n随便");
                    updateUserWChatStatus(ChatStatus.SELECTING_DESTINY);
                } else {
                    rtn = boxTextWxReturnMessage("小优好笨,没看懂,能重新说说吗");
                }
                break;
            case SELECTING_DESTINY:
                String destiny = WChatContext.getInputContent().trim();
                rtn = boxTextWxReturnMessage("好的,那能描述一下这方面的近况吗?");
                updateUserWChatStatus(ChatStatus.DESCRIBING_SELF);
                break;
            case DESCRIBING_SELF:
                String describe = WChatContext.getInputContent().trim();
                if (StringUtils.equals(describe, "没了")) {
                    rtn = boxTextWxReturnMessage("嗯嗯,想预测什么时期的运势呢?\n一周内\n一月内\n一年内\n随便预测");
                    updateUserWChatStatus(ChatStatus.SELECTING_TIME);
                } else {
                    rtn = boxTextWxReturnMessage("嗯嗯,小优在听,还有吗?");
                }
                break;
            case SELECTING_TIME:
                rtn = boxTextWxReturnMessage("知道辽,想要测得准,能提供一下一些个人信息吗,我问你答\n可以\n算了吧");
                updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
                break;
            case REPLENISH_SELF_DETAIL:
                updateUserWChatStatus(ChatStatus.PREDICTING);
                predict();
                break;
            case PREDICTING:
                rtn = boxTextWxReturnMessage("小优还在施法中,请耐心等一等哦");
                break;

        }
        return rtn;
    }

    private WxMpXmlMessage boxTextWxReturnMessage(String content) {
        WChatContext wChatContext = WChatContext.get();
        WxMpXmlMessage returnMsg = new WxMpXmlMessage();
        returnMsg.setFromUser(wChatContext.getWitchWChatService());
        returnMsg.setToUser(wChatContext.getUser());
        returnMsg.setContent(content);
        returnMsg.setMsgType(WxConsts.XmlMsgType.TEXT);
        return returnMsg;
    }


    private ChatStatus getUserWChatStatus() {
        ChatStatus chatStatus = wChatCache.getIfPresent(WChatContext.getWChatUser());
        return chatStatus == null ? ChatStatus.JUST_ENTER : chatStatus;
    }

    private void updateUserWChatStatus(ChatStatus status) {
        wChatCache.put(WChatContext.getWChatUser(), status);
    }

    private void predict() {
        //mock

    }

//    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
//    private void obtainwChatAccessToken() {
//
//    }

}
