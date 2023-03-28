package com.xfd;

import com.xfd.entity.UserDescribe;
import com.xfd.service.GPTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class MyWebService {

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


}