package com.xfd.wChat.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.xfd.common.UserCacheService;
import com.xfd.common.dao.UserCommonInfo;
import com.xfd.openai.service.GPTService;
import com.xfd.openai.service.WxChatStatusRouter;
import com.xfd.wChat.practise.ChatStatus;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
@Slf4j
public class WxChatService {

    @Value("${wChat.appID}")
    private String appID;
    @Value("${wChat.appSecret}")
    private String appSecret;

    private WxMpService wxMpService;

    @Autowired
    private UserCacheService userCacheService;

    private WxChatStatusRouter allMissRouter;

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
        fillRouterMap();
        allMissRouter = new AllMissRouter();
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
                returnMsg = boxTextWxReturnMessage("只因听不懂,只看得懂文字哦");
                break;
            default:
                returnMsg = boxTextWxReturnMessage("只因还不够智能,请再次输入");
                break;
        }
        String rtn = XStreamTransformer.toXml(WxMpXmlMessage.class, returnMsg);
        log.warn("wx_out_meg = {}", rtn);
        return rtn;
    }

    private WxMpXmlMessage processTextMsg() {
        ChatStatus chatStatus = getUserWChatStatus();
        WxChatStatusRouter statusRouter = routerMap.getOrDefault(chatStatus, allMissRouter);
        statusRouter.action();
        log.info("practice_contextInfo:status = {},whichProcessor={}", chatStatus, statusRouter.getClass().getSimpleName());
        boxTextWxMsg(WChatContext.getWxOutMsg());
        return WChatContext.getWxOutMsg();
    }


//    @Autowired
//    UserCommonInfoMapper userCommonInfoMapper;

    private static Set<String> validDestinySelect = Sets.newHashSet("1", "2", "3", "4", "5", "6",
        "事业", "爱情", "友情", "财运", "其他", "随便");

    private WxMpXmlMessage boxTextWxReturnMessage(String content) {
        WChatContext wChatContext = WChatContext.get();
        WxMpXmlMessage returnMsg = new WxMpXmlMessage();
        returnMsg.setFromUser(wChatContext.getWitchWChatService());
        returnMsg.setToUser(wChatContext.getUser());
        returnMsg.setContent(content);
        returnMsg.setMsgType(WxConsts.XmlMsgType.TEXT);
        returnMsg.setCreateTime(System.currentTimeMillis());
        return returnMsg;
    }

    private void boxTextWxMsg(WxMpXmlMessage wxMpXmlMessage) {
        WChatContext wChatContext = WChatContext.get();
        wxMpXmlMessage.setFromUser(wChatContext.getWitchWChatService());
        wxMpXmlMessage.setToUser(wChatContext.getUser());
        wxMpXmlMessage.setMsgType(WxConsts.XmlMsgType.TEXT);
        wxMpXmlMessage.setCreateTime(System.currentTimeMillis());
    }


    private ChatStatus getUserWChatStatus() {
        return userCacheService.getCurrentChatStatus();
    }

    private void updateUserWChatStatus(ChatStatus status) {
        userCacheService.updateChatStatus(status);
    }

    @Autowired
    GPTService gptService;

    private void predict() {
        //mock

        WChatContext wChatContext = WChatContext.get();
        UserCommonInfo userCommonInfo = userCacheService.getUserCache(REPLENISH_SELF_DETAIL);

        String time = userCacheService.getUserCache("practice_time");
        String currentStatus = userCacheService.getUserCache("practice_recent_info");
        String destiny = userCacheService.getUserCache("practice_destiny");

        String timeStr = time != null ? "接下来" + time : "";
        String destinyStr = destiny != null ? "有关" + destiny + "方面的" : "";
        String query = String.format("我叫%s,生日是%s,目前的工作是%s,身高体重是%s," +
                "而我目前的状况是%s,能否帮我预测一下%s%s运势",
            userCommonInfo.getName(),
            userCommonInfo.getBirthday(),
            userCommonInfo.getJob(),
            userCommonInfo.getHeightWeight(),
            currentStatus,
            timeStr,
            destinyStr);

        ListenableFutureTask<String> futureTask = ListenableFutureTask.create((Callable<String>) () -> {

            try {
                WChatContext.setDesigned(wChatContext);
                String result = gptService.practise(query);
                updateUserWChatStatus(ChatStatus.PREDICT_SUCCESS);
                userCacheService.updateUserCache(practiseResultKey, result);
                return result;
            } catch (Throwable e) {
                updateUserWChatStatus(ChatStatus.PREDICT_FAIL);
                return null;
            } finally {
                WChatContext.clear();
            }

        });
        scheduledExecutorService.schedule(futureTask, 10, TimeUnit.MILLISECONDS);

    }

    static String practiseResultKey = "practise_result";


    @Value("${wxChat.content.menu}")
    private String menuContent;

    private void firstHelpAnswer() {
        WChatContext.setAnswerContent("这里是只因,有什么可以帮您?\n" +
            "预测运势请输入\"1,运势\"\n" +
            "查看旅游攻略请输入\"2,旅游\"\n请输入前面的数字");
        updateUserWChatStatus(ChatStatus.SELECTING_MENU);
    }

    private void selectWhich2PracticeAnswer() {
        WChatContext.setAnswerContent("只因明白了,你想预测哪方面的运势捏?\n1,事业\n2,爱情\n3,友情\n4,财运\n5,随便\n请输入前面的数字");
        updateUserWChatStatus(ChatStatus.SELECTING_DESTINY);
    }

    private void miss() {
        WChatContext.setAnswerContent("存在于虚空的选项呢,请重新选择...");
    }

    private void selectedWhich2PracticeAnswer(String which2practice) {
        userCacheService.updateUserCache("practice_destiny", which2practice);
        WChatContext.setAnswerContent("好的,那能描述一下这方面的近况吗?如果不想请输入\"没了\"");
        updateUserWChatStatus(ChatStatus.DESCRIBING_SELF);
    }

    private void allMiss() {
        WChatContext.setAnswerContent("嗯?");
    }

    @Autowired
    @Qualifier("scheduledExecutorService")
    private ScheduledExecutorService scheduledExecutorService;

    Map<ChatStatus, WxChatStatusRouter> routerMap = Maps.newHashMap();

    private void fillRouterMap() {
        routerMap.put(ChatStatus.JUST_ENTER, new JustEnterRouter());
        routerMap.put(ChatStatus.SELECTING_MENU, new SelectMenuRouter());
        routerMap.put(ChatStatus.SELECTING_DESTINY, new SelectDestinyRouter());
        routerMap.put(ChatStatus.DESCRIBING_SELF, new DescribeSelfRouter());
        routerMap.put(ChatStatus.SELECTING_PRACTISE_TIME, new SelectPracticeTimeRouter());
        routerMap.put(ChatStatus.REPLENISH_SELF_DETAIL, new ReplenishUserDetailRouter());
        routerMap.put(ChatStatus.PREDICTING, new PracticingRouter());
        routerMap.put(ChatStatus.PREDICT_SUCCESS, new PracticeSuccessRouter());
        routerMap.put(ChatStatus.PREDICT_FAIL, new PracticeFailRouter());
    }

    private class JustEnterRouter implements WxChatStatusRouter {

        @Override
        public void action() {
            if (StringUtils.equals(WChatContext.getInputContent(), "运势")) {
                selectWhich2PracticeAnswer();
            } else {
                firstHelpAnswer();
            }
        }
    }

    private class SelectMenuRouter implements WxChatStatusRouter {
        @Override
        public void action() {
            if (StringUtils.equals(WChatContext.getInputContent(), "1") ||
                StringUtils.equals(WChatContext.getInputContent(), "运势")) {
                selectWhich2PracticeAnswer();
            } else {
                miss();
            }
        }
    }

    private class SelectDestinyRouter implements WxChatStatusRouter {
        @Override
        public void action() {
            if (StringUtils.equals(WChatContext.getInputContent(), "1") ||
                StringUtils.equals(WChatContext.getInputContent(), "事业")) {
                selectedWhich2PracticeAnswer("事业");
            } else if (StringUtils.equals(WChatContext.getInputContent(), "2") ||
                StringUtils.equals(WChatContext.getInputContent(), "爱情")) {
                selectedWhich2PracticeAnswer("爱情");
            } else if (StringUtils.equals(WChatContext.getInputContent(), "3") ||
                StringUtils.equals(WChatContext.getInputContent(), "友情")) {
                selectedWhich2PracticeAnswer("友情");
            } else if (StringUtils.equals(WChatContext.getInputContent(), "4") ||
                StringUtils.equals(WChatContext.getInputContent(), "财运")) {
                selectedWhich2PracticeAnswer("财运");
            } else if (StringUtils.equals(WChatContext.getInputContent(), "5") ||
                StringUtils.equals(WChatContext.getInputContent(), "随便")) {
                selectedWhich2PracticeAnswer((String) null);
            } else {
                miss();
            }
        }
    }

    private class DescribeSelfRouter implements WxChatStatusRouter {
        @Override
        public void action() {
            if (StringUtils.equals(WChatContext.getInputContent(), "没了")) {
                WChatContext.setAnswerContent("嗯嗯,想预测什么时期的运势呢?\n1,一周内\n2,一月内\n3,一年内\n4,随便预测");
                updateUserWChatStatus(ChatStatus.SELECTING_PRACTISE_TIME);
            } else {
                String describe = WChatContext.getInputContent();
                String cacheDescribe = userCacheService.getUserCache("practice_recent_info");
                if (cacheDescribe == null) {
                    userCacheService.updateUserCache("practice_recent_info", describe);
                } else {
                    userCacheService.updateUserCache("practice_recent_info", cacheDescribe + "," + describe);
                }
                WChatContext.setAnswerContent("嗯嗯,只因在听,还有吗?没有请输入\"没了\"");
            }
        }
    }

    private class SelectPracticeTimeRouter implements WxChatStatusRouter {
        @Override
        public void action() {
            if (StringUtils.equals(WChatContext.getInputContent(), "1") ||
                StringUtils.equals(WChatContext.getInputContent(), "一周内")) {
                userCacheService.updateUserCache("practice_time", "一周内");
                WChatContext.setAnswerContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
                updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
            } else if (StringUtils.equals(WChatContext.getInputContent(), "2") ||
                StringUtils.equals(WChatContext.getInputContent(), "一月内")) {
                userCacheService.updateUserCache("practice_time", "一月内");
                WChatContext.setAnswerContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
                updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
            } else if (StringUtils.equals(WChatContext.getInputContent(), "3") ||
                StringUtils.equals(WChatContext.getInputContent(), "一年内")) {
                userCacheService.updateUserCache("practice_time", "一年内");
                WChatContext.setAnswerContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
                updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
            } else if (StringUtils.equals(WChatContext.getInputContent(), "4") ||
                StringUtils.equals(WChatContext.getInputContent(), "随便预测")) {
                WChatContext.setAnswerContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
                updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
            } else {
                miss();
            }
        }
    }

    private class ReplenishUserDetailRouter implements WxChatStatusRouter {
        @Override
        public void action() {
            if (userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == null &&
                (StringUtils.equals("2", WChatContext.getInputContent()) || StringUtils.equals("算了吧", WChatContext.getInputContent()))) {
                updateUserWChatStatus(ChatStatus.PREDICTING);
                WChatContext.setAnswerContent("只因开始施法了,请稍后输入任意内容查询");
                predict();
            } else if (userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == null &&
                (StringUtils.equals("1", WChatContext.getInputContent()) || StringUtils.equals("可以", WChatContext.getInputContent()))) {
                userCacheService.updateUserCache(REPLENISH_SELF_DETAIL, new UserCommonInfo());
                userCacheService.updateUserCache(REPLENISH_SELF_DETAIL_STEP, 1);
                WChatContext.setAnswerContent("您的名字是?(真名或者昵称)");
            } else if (userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == null) {
                miss();
            } else if ((int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == 1) {
                ((UserCommonInfo) userCacheService.getUserCache(REPLENISH_SELF_DETAIL)).setName(WChatContext.getInputContent());
                userCacheService.updateUserCache(REPLENISH_SELF_DETAIL_STEP, 2);
                WChatContext.setAnswerContent("您的生日是?");
            } else if ((int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == 2) {
                ((UserCommonInfo) userCacheService.getUserCache(REPLENISH_SELF_DETAIL)).setBirthday(WChatContext.getInputContent());
                userCacheService.updateUserCache(REPLENISH_SELF_DETAIL_STEP, 3);
                WChatContext.setAnswerContent("您目前的工作是?");
            } else if ((int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == 3) {
                ((UserCommonInfo) userCacheService.getUserCache(REPLENISH_SELF_DETAIL)).setJob(WChatContext.getInputContent());
                userCacheService.updateUserCache(REPLENISH_SELF_DETAIL_STEP, 4);
                WChatContext.setAnswerContent("您的身高体重是?");
            } else if ((int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == 4) {
                ((UserCommonInfo) userCacheService.getUserCache(REPLENISH_SELF_DETAIL)).setHeightWeight(WChatContext.getInputContent());
                userCacheService.updateUserCache(REPLENISH_SELF_DETAIL_STEP, 5);
                updateUserWChatStatus(ChatStatus.PREDICTING);
                WChatContext.setAnswerContent("只因开始施法了,请稍后输入任意内容查询");
                predict();
            }
        }
    }

    private class PracticingRouter implements WxChatStatusRouter {
        @Override
        public void action() {
            WChatContext.setAnswerContent("只因正在施法中,请稍后输入任意内容查询");
        }
    }

    private class PracticeSuccessRouter implements WxChatStatusRouter {
        @Override
        public void action() {
            String practiceResult = userCacheService.getUserCache(practiseResultKey);
            WChatContext.setAnswerContent(practiceResult);
            updateUserWChatStatus(ChatStatus.JUST_ENTER);
        }
    }

    private class PracticeFailRouter implements WxChatStatusRouter {
        @Override
        public void action() {
            WChatContext.setAnswerContent("只因魔力不足失败了...请重新预测吧");
            updateUserWChatStatus(ChatStatus.JUST_ENTER);
        }
    }

    private String REPLENISH_SELF_DETAIL_STEP = "REPLENISH_SELF_DETAIL_STEP";
    private String REPLENISH_SELF_DETAIL = "REPLENISH_SELF_DETAIL";


    private class AllMissRouter implements WxChatStatusRouter {
        @Override
        public void action() {
            allMiss();
        }
    }


//    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
//    private void obtainwChatAccessToken() {
//
//    }

}
