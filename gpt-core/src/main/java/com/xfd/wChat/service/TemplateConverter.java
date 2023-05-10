package com.xfd.wChat.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateConverter {

    @Value("${robot.name}")
    private String robotName;

    public String convert(String origin) {
        Matcher matcher = robotNamePlaceHolder.matcher(origin);
        while (matcher.find()) {
            origin = matcher.replaceAll("只因");
        }
        return origin;
    }

    private static Pattern robotNamePlaceHolder = Pattern.compile("\\$\\{robotName}");

    public static void main(String[] args) {

    }

}
