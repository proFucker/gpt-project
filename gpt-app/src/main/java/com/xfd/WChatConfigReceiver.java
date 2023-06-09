package com.xfd;

import com.xfd.wChat.service.WxChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@RestController
public class WChatConfigReceiver {

    @Autowired
    private WxChatService wxChatService;

    @Autowired
    private Environment environment;

    @Value("${wChat.token}")
    private String token;

    @RequestMapping(value = "/wx", method = RequestMethod.POST)
    ResponseEntity<String> wxDataReceiver(@RequestParam(required = false) String signature,
                                          @RequestParam(required = false) String timestamp,
                                          @RequestParam(required = false) String nonce,
                                          @RequestBody String xmlData) {
        return ResponseEntity.ok(wxChatService.processWXPushData(xmlData));
    }

    @RequestMapping(value = "/wx", method = RequestMethod.GET)
    ResponseEntity<String> serviceConfirm(@RequestParam String signature,
                                          @RequestParam String timestamp,
                                          @RequestParam String nonce,
                                          @RequestParam String echostr) {
        if (checkSignature(signature, timestamp, nonce, token)) {
            return ResponseEntity.ok(echostr);
        } else {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).build();
        }
    }

    public boolean checkSignature(String signature, String timestamp, String nonce, String token) {
        String checktext = null;
        if (null != signature) {
            //对ToKen,timestamp,nonce 按字典排序
            String[] paramArr = new String[]{token, timestamp, nonce};
            Arrays.sort(paramArr);
            //将排序后的结果拼成一个字符串
            String content = paramArr[0].concat(paramArr[1]).concat(paramArr[2]);

            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                //对接后的字符串进行sha1加密
                byte[] digest = md.digest(content.toString().getBytes());
                checktext = byteToStr(digest);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        //将加密后的字符串与signature进行对比
        return checktext != null ? checktext.equals(signature.toUpperCase()) : false;
    }

    private String byteToStr(byte[] byteArrays) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < byteArrays.length; i++) {
            sb.append(byteToHexStr(byteArrays[i]));
        }
        return sb.toString();
    }

    private String byteToHexStr(byte myByte) {
        char[] Digit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] tampArr = new char[2];
        tampArr[0] = Digit[(myByte >>> 4) & 0X0F];
        tampArr[1] = Digit[myByte & 0X0F];
        return new String(tampArr);
    }
}
