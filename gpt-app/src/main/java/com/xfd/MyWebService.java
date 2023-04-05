package com.xfd;

import com.xfd.common.dao.ChatRecord;
import com.xfd.common.mapper.ChatRecordMapper;
import com.xfd.openai.entity.UserDescribe;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
public class MyWebService implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @RequestMapping(value = "/baseInfo", method = RequestMethod.POST)
    ResponseEntity<String> userBaseDescribe(@RequestBody UserDescribe userDescribe) {
        return ResponseEntity.ok("请问您还有什么补充的吗?比如最近的喜事或者烦心事");
    }

//    @Autowired
//    private GPTService gptService;
//
//    @RequestMapping(value = "/practise", method = RequestMethod.POST)
//    ResponseEntity<String> practise(@RequestBody String extraDescribe) {
//        String gptResponse = gptService.doChat(new UserDescribe());
//        return ResponseEntity.ok(gptResponse);
//    }


    @Autowired
    ChatRecordMapper chatRecordMapper;

    @RequestMapping(value = "/test1", method = RequestMethod.GET)
    ResponseEntity<String> test1(@RequestParam String key) {

        return ResponseEntity.ok(key);
    }

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    ResponseEntity<String> test1() {
        List<ChatRecord> chatRecords = chatRecordMapper.selectCurrentChat("shit", "hello");
        return ResponseEntity.ok("hello_world");
    }

    @RequestMapping(value = "/hello", method = RequestMethod.POST)
    ResponseEntity<String> test2(@RequestParam(required = false) String shit) {
        boolean ii = chatRecordMapper.batchSaveChatRecord(Arrays.asList(new ChatRecord()
            .setRole("i").setContent(UUID.randomUUID().toString()).setUserId("shit").setContextKey("hello")));
        System.out.println(shit);
        return ResponseEntity.ok("hello_world_post");
    }

}