package com.xfd.weChat.service;

import com.google.gson.Gson;
import lombok.Getter;
import me.chanjar.weixin.common.util.http.okhttp.DefaultOkHttpClientBuilder;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceOkHttpImpl;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.config.WxMpConfigStorage;
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
//@EnableScheduling
public class WeChatService {

    @Value("${weChat.appID}")
    private String appID;
    @Value("${weChat.appSecret}")
    private String appSecret;


    @Bean
    public WxMpService configWeChatService() {
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
        } catch (Exception e) {
            return null;
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
        System.out.println(new Gson().toJson(wxMpXmlMessage));
        return "";
    }

//    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
//    private void obtainWeChatAccessToken() {
//
//    }

}
