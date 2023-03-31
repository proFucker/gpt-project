package com.xfd.wChat.service;

import com.google.gson.Gson;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
@Slf4j
public class WChatService {

    @Value("${wChat.appID}")
    private String appID;
    @Value("${wChat.appSecret}")
    private String appSecret;


    @Bean
    public WxMpService configWChatService() {
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
        return wxMpServiceOkHttp;
    }

    @Autowired
    private WxMpService wxMpService;

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
        System.out.println(xmlData);
        WxMpXmlMessage wxMpXmlMessage = XStreamTransformer.fromXml(WxMpXmlMessage.class, xmlData);
        WxMpXmlMessage returnMsg = new WxMpXmlMessage();
        returnMsg.setFromUser("gh_ab5d4378c71d");
        returnMsg.setToUser(wxMpXmlMessage.getToUser());
        returnMsg.setContent(wxMpXmlMessage.getContent());
        returnMsg.setCreateTime(System.currentTimeMillis());
        returnMsg.setMsgType(WxConsts.XmlMsgType.TEXT);
        String rtn = XStreamTransformer.toXml(WxMpXmlMessage.class, returnMsg);
        System.out.println(rtn);
//        System.out.println(new Gson().toJson(wxMpXmlMessage));
        return rtn;
    }

//    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
//    private void obtainwChatAccessToken() {
//
//    }

}
