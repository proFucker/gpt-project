package com.xfd;

import com.xfd.openai.entity.UserDescribe;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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


    @RequestMapping(value = "/test1", method = RequestMethod.GET)
    ResponseEntity<String> test1(@RequestParam String key) {
        return ResponseEntity.ok(key);
    }

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    ResponseEntity<String> test1() {
        return ResponseEntity.ok("hello_world");
    }

    @RequestMapping(value = "/hello", method = RequestMethod.POST)
    ResponseEntity<String> test2(@RequestParam(required = false) String shit) {
        System.out.println(shit);
        return ResponseEntity.ok("hello_world_post");
    }

}