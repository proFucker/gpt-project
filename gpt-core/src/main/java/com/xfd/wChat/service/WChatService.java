package com.xfd.wChat.service;

import com.google.common.cache.Cache;
import com.google.common.collect.Sets;
import com.xfd.common.UserCacheService;
import com.xfd.common.dao.UserCommonInfo;
import com.xfd.common.mapper.UserCommonInfoMapper;
import com.xfd.wChat.practise.ChatStatus;
import com.xfd.common.WChatContext;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.util.http.okhttp.DefaultOkHttpClientBuilder;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceOkHttpImpl;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.config.impl.WxMpMapConfigImpl;
import me.chanjar.weixin.mp.util.xml.XStreamTransformer;
import okhttp3.ConnectionPool;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
@Slf4j
public class WChatService {

    @Value("${wChat.appID}")
    private String appID;
    @Value("${wChat.appSecret}")
    private String appSecret;

    private WxMpService wxMpService;

    @Autowired
    private UserCacheService userCacheService;

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
        log.warn("wx_in_meg = {}", xmlData);
        WxMpXmlMessage wxMpXmlMessage = XStreamTransformer.fromXml(WxMpXmlMessage.class, xmlData);
        wxMpXmlMessage.setContent(wxMpXmlMessage.getContent().trim());
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
        String rtn = XStreamTransformer.toXml(WxMpXmlMessage.class, returnMsg);
        log.warn("wx_out_meg = {}", rtn);
        return rtn;
    }

    private WxMpXmlMessage processTextMsg() {
        ChatStatus chatStatus = getUserWChatStatus();
        WxMpXmlMessage answer = new WxMpXmlMessage();
        UserCommonInfo userCommonInfo = null;
        switch (chatStatus) {
            case JUST_ENTER:
                if (StringUtils.equals(WChatContext.getInputContent(), "运势")) {
                    answer.setContent("小优明白了,你想预测哪方面的运势捏?\n1,事业\n2,爱情\n3,友情\n4,财运\n5,其他\n6,随便\n请输入前面的数字");
                    updateUserWChatStatus(ChatStatus.SELECTING_DESTINY);
                } else {
                    answer.setContent("这里是小优,有什么可以帮您?\n" +
                        "预测运势请输入\"1,运势\"\n" +
                        "查看旅游攻略请输入\"2,旅游\"\n请输入前面的数字");
                    updateUserWChatStatus(ChatStatus.SELECTING_MENU);
                }
                break;
            case SELECTING_MENU:
                if (StringUtils.equals(WChatContext.getInputContent(), "1") ||
                    StringUtils.equals(WChatContext.getInputContent(), "运势")) {
                    answer.setContent("小优明白了,你想预测哪方面的运势捏?\n1,事业\n2,爱情\n3,友情\n4,财运\n5,其他\n6,随便\n请输入前面的数字");
                    updateUserWChatStatus(ChatStatus.SELECTING_DESTINY);
                } else {
                    answer.setContent("存在于虚空的选项呢,请重新选择...");
                }
                break;
            case SELECTING_DESTINY:
                switch (WChatContext.getInputContent()) {
                    case "1":
                    case "事业":
                        userCacheService.updateUserCache("practice_destiny", "事业");
                        answer.setContent("好的,那能描述一下这方面的近况吗?如果不想请输入\"没了\"");
                        break;
                    case "2":
                    case "爱情":
                        userCacheService.updateUserCache("practice_destiny", "爱情");
                        answer.setContent("好的,那能描述一下这方面的近况吗?如果不想请输入\"没了\"");
                        break;
                    case "3":
                    case "友情":
                        userCacheService.updateUserCache("practice_destiny", "友情");
                        answer.setContent("好的,那能描述一下这方面的近况吗?如果不想请输入\"没了\"");
                        break;
                    case "4":
                    case "财运":
                        userCacheService.updateUserCache("practice_destiny", "财运");
                        answer.setContent("好的,那能描述一下这方面的近况吗?如果不想请输入\"没了\"");
                        break;
                    case "5":
                    case "其他":
                    case "6":
                    case "随便":
                        answer.setContent("好的,那能描述一下这方面的近况吗?如果不想请输入\"没了\"");
                        break;
                    default:
                        answer.setContent("存在于虚空的选项呢,请重新选择...");
                        break;
                }
                break;
            case DESCRIBING_SELF:
                String describe = WChatContext.getInputContent();
                if (StringUtils.equals(describe, "没了")) {
                    answer.setContent("嗯嗯,想预测什么时期的运势呢?\n1,一周内\n2,一月内\n3,一年内\n4,随便预测");
                    updateUserWChatStatus(ChatStatus.SELECTING_PRACTISE_TIME);
                } else {
                    String cacheDescribe = userCacheService.getUserCache("practice_recent_info");
                    if (cacheDescribe == null) {
                        userCacheService.updateUserCache("practice_recent_info", describe);
                    } else {
                        userCacheService.updateUserCache("practice_recent_info", cacheDescribe + "," + describe);
                    }
                    answer.setContent("嗯嗯,小优在听,还有吗?没有请输入\"没了\"");
                }
                break;
            case SELECTING_PRACTISE_TIME:

                switch (WChatContext.getInputContent()) {
                    case "1":
                    case "一周内":
                        userCacheService.updateUserCache("practice_time", "一周内");
//                        userCommonInfo = userCommonInfoMapper.selectUserCommonInfoByUserId(WChatContext.getWChatUser());
//                        if (userCommonInfo == null) {
//                        userCommonInfoMapper.insertNewUserCommonInfo(WChatContext.getWChatUser(), System.currentTimeMillis());
                        answer.setContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
//                        } else {
//                            if(StringUtils.isBlank(userCommonInfo.getBirthday())
//                        }
                        updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
                        break;
                    case "2":
                    case "一月内":
                        userCacheService.updateUserCache("practice_time", "一月内");
//                        userCommonInfo = userCommonInfoMapper.selectUserCommonInfoByUserId(WChatContext.getWChatUser());
                        answer.setContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
                        updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
                        break;
                    case "3":
                    case "一年内":
                        userCacheService.updateUserCache("practice_time", "一年内");
//                        userCommonInfo = userCommonInfoMapper.selectUserCommonInfoByUserId(WChatContext.getWChatUser());
                        answer.setContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
                        updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
                        break;
                    case "4":
                    case "随便预测":
                        answer.setContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
                        updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
                        break;
                    default:
                        answer.setContent("存在于虚空的选项呢,请重新选择...");
                        break;
                }
                break;
            case REPLENISH_SELF_DETAIL:
                Integer detailStep = userCacheService.getUserCache("REPLENISH_SELF_DETAIL_STEP");
                if (detailStep != null) {
                    switch (detailStep) {
                        case 1:
                            ((UserCommonInfo) userCacheService.getUserCache("REPLENISH_SELF_DETAIL")).setName(WChatContext.getInputContent());
                            userCacheService.updateUserCache("REPLENISH_SELF_DETAIL_STEP", 2);
                            answer.setContent("您的生日是?");
                            break;
                        case 2:
                            ((UserCommonInfo) userCacheService.getUserCache("REPLENISH_SELF_DETAIL")).setBirthday(WChatContext.getInputContent());
                            userCacheService.updateUserCache("REPLENISH_SELF_DETAIL_STEP", 3);
                            updateUserWChatStatus(ChatStatus.PREDICTING);
                            answer.setContent("小优开始施法了,请耐心等一下");
                            predict();
                            break;
                    }
                } else {
                    switch (WChatContext.getInputContent()) {
                        case "1":
                        case "可以":
                            userCacheService.updateUserCache("REPLENISH_SELF_DETAIL", new UserCommonInfo());
                            userCacheService.updateUserCache("REPLENISH_SELF_DETAIL_STEP", 1);
                            answer.setContent("您的名字是?(真名或者昵称)");
                            break;
                        case "2":
                        case "算了吧":
                            updateUserWChatStatus(ChatStatus.PREDICTING);
                            answer.setContent("小优开始施法了,请耐心等一下");
                            predict();
                            break;
                        default:
                            answer.setContent("存在于虚空的选项呢,请重新选择...");
                            break;
                    }
                }
                break;
            case PREDICTING:
                answer.setContent("小优还在施法中,请耐心等一等哦");
                break;

        }
        return answer;
    }


    @Autowired
    UserCommonInfoMapper userCommonInfoMapper;

    private static Set<String> validDestinySelect = Sets.newHashSet("1", "2", "3", "4", "5", "6",
        "事业", "爱情", "友情", "财运", "其他", "随便");

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
        return userCacheService.getCurrentChatStatus();
    }

    private void updateUserWChatStatus(ChatStatus status) {
        userCacheService.updateChatStatus(status);
    }

    private void predict() {
        //mock

        WChatContext wChatContext = WChatContext.get();
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    WChatContext.setDesigned(wChatContext);
                    WxMpKefuMessage wxMpKefuMessage = new WxMpKefuMessage();
                    wxMpKefuMessage.setContent("你好,你的运势预测出来了");
                    wxMpKefuMessage.setToUser(wChatContext.getUser());
                    wxMpKefuMessage.setMsgType(WxConsts.KefuMsgType.TEXT);
                    boolean success = wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
                    if (!success) {
                        log.error("kefu_error");
                    } else {
                        updateUserWChatStatus(ChatStatus.JUST_ENTER);
                    }
                } catch (Exception e) {
                    log.error("kefu_error", e);
                } finally {
                    WChatContext.clear();
                }

            }
        }, 10, TimeUnit.SECONDS);
    }

    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

//    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
//    private void obtainwChatAccessToken() {
//
//    }

}
