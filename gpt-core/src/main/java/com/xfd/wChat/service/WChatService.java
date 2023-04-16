package com.xfd.wChat.service;

import com.google.common.cache.Cache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.xfd.common.StatusMatcher;
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
import java.util.List;
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

    private List<StatusMatcher> statusMatchers;

    private StatusMatcher allMissMatcher;

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

        statusMatchers = Lists.newArrayList();
        statusMatchers.add(new JustEnterMatcher1());
        statusMatchers.add(new JustEnterMatcher2());
        statusMatchers.add(new SelectMenuMatcher1());
        statusMatchers.add(new SelectMenuMatcher2());
        statusMatchers.add(new SelectDestinyMatcher1());
        statusMatchers.add(new SelectDestinyMatcher2());
        statusMatchers.add(new SelectDestinyMatcher3());
        statusMatchers.add(new SelectDestinyMatcher4());
        statusMatchers.add(new SelectDestinyMatcher5());
        statusMatchers.add(new SelectDestinyMatcher6());
        statusMatchers.add(new DescribeSelfMatcher1());
        statusMatchers.add(new DescribeSelfMatcher2());
        statusMatchers.add(new SelectPracticeTimeMatcher1());
        statusMatchers.add(new SelectPracticeTimeMatcher2());
        statusMatchers.add(new SelectPracticeTimeMatcher3());
        statusMatchers.add(new SelectPracticeTimeMatcher4());
        statusMatchers.add(new SelectPracticeTimeMatcher5());
        statusMatchers.add(new ReplenishUserDetailMatcher1());
        statusMatchers.add(new ReplenishUserDetailMatcher2());
        statusMatchers.add(new ReplenishUserDetailMatcher3());
        statusMatchers.add(new ReplenishUserDetailMatcher4Name());
        statusMatchers.add(new ReplenishUserDetailMatcher4Birthday());
        statusMatchers.add(new ReplenishUserDetailMatcher5Job());
        statusMatchers.add(new ReplenishUserDetailMatcher6HeightWeight());
        statusMatchers.add(new PracticingMatcher());
        allMissMatcher = new AllMissMatcher();
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
        StatusMatcher whichMatch = allMissMatcher;
        for (StatusMatcher statusMatcher : statusMatchers) {
            if (statusMatcher.match()) {
                statusMatcher.action();
                whichMatch = statusMatcher;
                break;
            }
        }
        if (whichMatch == allMissMatcher) {
            allMissMatcher.action();
        }
        log.info("practice_contextInfo:status = {},whichProcessor={}", chatStatus, whichMatch.getClass().getSimpleName());
        boxTextWxMsg(WChatContext.getWxOutMsg());

//        WxMpXmlMessage answer = new WxMpXmlMessage();
//        UserCommonInfo userCommonInfo = null;
//        switch (chatStatus) {
//
//
//            case PREDICTING:
//                answer.setContent("小优还在施法中,请耐心等一等哦");
//                break;
//
//        }

//        boxTextWxMsg(answer);
        return WChatContext.getWxOutMsg();
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

        log.error("start_ky");
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                log.error("start_ky_1");
                try {
                    WChatContext.setDesigned(wChatContext);
                    log.error("start_ky_2");
                    WxMpKefuMessage wxMpKefuMessage = new WxMpKefuMessage();
                    log.error("start_ky_3");
                    wxMpKefuMessage.setContent(String.format("你好,你的运势预测出来了,请求串是:%s", query));
                    wxMpKefuMessage.setToUser(wChatContext.getUser());
                    wxMpKefuMessage.setMsgType(WxConsts.KefuMsgType.TEXT);
                    boolean success = wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
                    if (!success) {
                        log.error("kefu_error");
                    } else {
                        log.error("kefu_success");
                        updateUserWChatStatus(ChatStatus.JUST_ENTER);
                    }
                } catch (Throwable e) {
                    log.error("kefu_error", e);
                } finally {
                    WChatContext.clear();
                }

            }
        }, 10, TimeUnit.SECONDS);
    }

    private void firstHelpAnswer() {
        WChatContext.setAnswerContent("这里是小优,有什么可以帮您?\n" +
            "预测运势请输入\"1,运势\"\n" +
            "查看旅游攻略请输入\"2,旅游\"\n请输入前面的数字");
        updateUserWChatStatus(ChatStatus.SELECTING_MENU);
    }

    private void selectWhich2PracticeAnswer() {
        WChatContext.setAnswerContent("小优明白了,你想预测哪方面的运势捏?\n1,事业\n2,爱情\n3,友情\n4,财运\n5,随便\n请输入前面的数字");
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

    private class JustEnterMatcher1 implements StatusMatcher {
        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.JUST_ENTER && StringUtils.equals(WChatContext.getInputContent(), "运势");
        }

        @Override
        public void action() {
            selectWhich2PracticeAnswer();
        }
    }

    private class JustEnterMatcher2 implements StatusMatcher {
        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.JUST_ENTER;
        }

        @Override
        public void action() {
            firstHelpAnswer();
        }
    }

    private class SelectMenuMatcher1 implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_MENU &&
                (StringUtils.equals(WChatContext.getInputContent(), "1") ||
                    StringUtils.equals(WChatContext.getInputContent(), "运势"));
        }

        @Override
        public void action() {
            selectWhich2PracticeAnswer();
        }
    }

    private class SelectMenuMatcher2 implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_MENU;

        }

        @Override
        public void action() {
            miss();
        }
    }

    private class SelectDestinyMatcher1 implements StatusMatcher {

        private final String keyWord = "事业";

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_DESTINY &&
                (StringUtils.equals(WChatContext.getInputContent(), "1") ||
                    StringUtils.equals(WChatContext.getInputContent(), keyWord));

        }

        @Override
        public void action() {
            selectedWhich2PracticeAnswer(keyWord);
        }
    }

    private class SelectDestinyMatcher2 implements StatusMatcher {

        private final String keyWord = "爱情";

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_DESTINY &&
                (StringUtils.equals(WChatContext.getInputContent(), "2") ||
                    StringUtils.equals(WChatContext.getInputContent(), keyWord));

        }

        @Override
        public void action() {
            selectedWhich2PracticeAnswer(keyWord);
        }
    }

    private class SelectDestinyMatcher3 implements StatusMatcher {

        private final String keyWord = "友情";

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_DESTINY &&
                (StringUtils.equals(WChatContext.getInputContent(), "3") ||
                    StringUtils.equals(WChatContext.getInputContent(), keyWord));

        }

        @Override
        public void action() {
            selectedWhich2PracticeAnswer(keyWord);
        }
    }

    private class SelectDestinyMatcher4 implements StatusMatcher {

        private final String keyWord = "财运";

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_DESTINY &&
                (StringUtils.equals(WChatContext.getInputContent(), "4") ||
                    StringUtils.equals(WChatContext.getInputContent(), keyWord));

        }

        @Override
        public void action() {
            selectedWhich2PracticeAnswer(keyWord);
        }
    }

    private class SelectDestinyMatcher5 implements StatusMatcher {

        private final String keyWord = "随便";

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_DESTINY &&
                (StringUtils.equals(WChatContext.getInputContent(), "5") ||
                    StringUtils.equals(WChatContext.getInputContent(), keyWord));
        }

        @Override
        public void action() {
            selectedWhich2PracticeAnswer((String) null);
        }
    }

    private class SelectDestinyMatcher6 implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_DESTINY;
        }

        @Override
        public void action() {
            miss();
        }
    }

    private class DescribeSelfMatcher1 implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.DESCRIBING_SELF &&
                StringUtils.equals(WChatContext.getInputContent(), "没了");
        }

        @Override
        public void action() {
            WChatContext.setAnswerContent("嗯嗯,想预测什么时期的运势呢?\n1,一周内\n2,一月内\n3,一年内\n4,随便预测");
            updateUserWChatStatus(ChatStatus.SELECTING_PRACTISE_TIME);
        }
    }

    private class DescribeSelfMatcher2 implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.DESCRIBING_SELF;
        }

        @Override
        public void action() {
            String describe = WChatContext.getInputContent();
            String cacheDescribe = userCacheService.getUserCache("practice_recent_info");
            if (cacheDescribe == null) {
                userCacheService.updateUserCache("practice_recent_info", describe);
            } else {
                userCacheService.updateUserCache("practice_recent_info", cacheDescribe + "," + describe);
            }
            WChatContext.setAnswerContent("嗯嗯,小优在听,还有吗?没有请输入\"没了\"");
        }
    }

    private class SelectPracticeTimeMatcher1 implements StatusMatcher {

        private final String keyWord = "一周内";

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_PRACTISE_TIME &&
                (StringUtils.equals(WChatContext.getInputContent(), "1") || StringUtils.equals(WChatContext.getInputContent(), keyWord));
        }

        @Override
        public void action() {
            userCacheService.updateUserCache("practice_time", keyWord);
            WChatContext.setAnswerContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
            updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
        }
    }

    private class SelectPracticeTimeMatcher2 implements StatusMatcher {

        private final String keyWord = "一月内";

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_PRACTISE_TIME &&
                (StringUtils.equals(WChatContext.getInputContent(), "2") || StringUtils.equals(WChatContext.getInputContent(), keyWord));
        }

        @Override
        public void action() {
            userCacheService.updateUserCache("practice_time", keyWord);
            WChatContext.setAnswerContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
            updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
        }
    }

    private class SelectPracticeTimeMatcher3 implements StatusMatcher {

        private final String keyWord = "一年内";

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_PRACTISE_TIME &&
                (StringUtils.equals(WChatContext.getInputContent(), "3") || StringUtils.equals(WChatContext.getInputContent(), keyWord));
        }

        @Override
        public void action() {
            userCacheService.updateUserCache("practice_time", keyWord);
            WChatContext.setAnswerContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
            updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
        }
    }

    private class SelectPracticeTimeMatcher4 implements StatusMatcher {

        private final String keyWord = "随便预测";

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_PRACTISE_TIME &&
                (StringUtils.equals(WChatContext.getInputContent(), "4") || StringUtils.equals(WChatContext.getInputContent(), keyWord));
        }

        @Override
        public void action() {
            WChatContext.setAnswerContent("知道辽,想要测得准,能提供一下一些个人信息吗,能让预测更加准确,我问你答。\n1,可以\n2,算了吧");
            updateUserWChatStatus(ChatStatus.REPLENISH_SELF_DETAIL);
        }
    }

    private class SelectPracticeTimeMatcher5 implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.SELECTING_PRACTISE_TIME;
        }

        @Override
        public void action() {
            miss();
        }
    }

    private class ReplenishUserDetailMatcher1 implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.REPLENISH_SELF_DETAIL &&
                userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == null &&
                (StringUtils.equals("2", WChatContext.getInputContent()) || StringUtils.equals("算了吧", WChatContext.getInputContent()));
        }

        @Override
        public void action() {
            updateUserWChatStatus(ChatStatus.PREDICTING);
            WChatContext.setAnswerContent("小优开始施法了,请耐心等一下");
            predict();
        }
    }

    private class ReplenishUserDetailMatcher2 implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.REPLENISH_SELF_DETAIL &&
                userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == null &&
                (StringUtils.equals("1", WChatContext.getInputContent()) || StringUtils.equals("可以", WChatContext.getInputContent()));
        }

        @Override
        public void action() {
            userCacheService.updateUserCache(REPLENISH_SELF_DETAIL, new UserCommonInfo());
            userCacheService.updateUserCache(REPLENISH_SELF_DETAIL_STEP, 1);
            WChatContext.setAnswerContent("您的名字是?(真名或者昵称)");
        }
    }

    private class ReplenishUserDetailMatcher3 implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.REPLENISH_SELF_DETAIL &&
                userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == null;
        }

        @Override
        public void action() {
            miss();
        }
    }

    private class ReplenishUserDetailMatcher4Name implements StatusMatcher {

        @Override
        public boolean match() {
            log.error("shit,{},{},{}",getUserWChatStatus() == ChatStatus.REPLENISH_SELF_DETAIL &&
                    (int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == 1,
                (int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == 1,
                (int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP));

            return getUserWChatStatus() == ChatStatus.REPLENISH_SELF_DETAIL &&
                (int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == 1;
        }

        @Override
        public void action() {
            ((UserCommonInfo) userCacheService.getUserCache(REPLENISH_SELF_DETAIL)).setName(WChatContext.getInputContent());
            userCacheService.updateUserCache(REPLENISH_SELF_DETAIL_STEP, 2);
            WChatContext.setAnswerContent("您的生日是?");
        }
    }

    private class ReplenishUserDetailMatcher4Birthday implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.REPLENISH_SELF_DETAIL &&
                (int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == 2;
        }

        @Override
        public void action() {
            ((UserCommonInfo) userCacheService.getUserCache(REPLENISH_SELF_DETAIL)).setBirthday(WChatContext.getInputContent());
            userCacheService.updateUserCache(REPLENISH_SELF_DETAIL_STEP, 3);
            WChatContext.setAnswerContent("您目前的工作是?");
        }
    }

    private class ReplenishUserDetailMatcher5Job implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.REPLENISH_SELF_DETAIL &&
                (int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == 3;
        }

        @Override
        public void action() {
            ((UserCommonInfo) userCacheService.getUserCache(REPLENISH_SELF_DETAIL)).setJob(WChatContext.getInputContent());
            userCacheService.updateUserCache(REPLENISH_SELF_DETAIL_STEP, 4);
            WChatContext.setAnswerContent("您的身高体重是?");
        }
    }

    private class ReplenishUserDetailMatcher6HeightWeight implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.REPLENISH_SELF_DETAIL &&
                (int) userCacheService.getUserCache(REPLENISH_SELF_DETAIL_STEP) == 4;
        }

        @Override
        public void action() {
            ((UserCommonInfo) userCacheService.getUserCache(REPLENISH_SELF_DETAIL)).setHeightWeight(WChatContext.getInputContent());
            userCacheService.updateUserCache(REPLENISH_SELF_DETAIL_STEP, 5);
            updateUserWChatStatus(ChatStatus.PREDICTING);
            WChatContext.setAnswerContent("小优开始施法了,请耐心等一下");
            predict();
        }
    }

    private class PracticingMatcher implements StatusMatcher {

        @Override
        public boolean match() {
            return getUserWChatStatus() == ChatStatus.PREDICTING;
        }

        @Override
        public void action() {
            WChatContext.setAnswerContent("小优正在施法中,请耐心等一下");
        }
    }

    private String REPLENISH_SELF_DETAIL_STEP = "REPLENISH_SELF_DETAIL_STEP";
    private String REPLENISH_SELF_DETAIL = "REPLENISH_SELF_DETAIL";


    private class AllMissMatcher implements StatusMatcher {
        @Override
        public boolean match() {
            return true;
        }

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
