package com.xfd.service;

import com.google.common.collect.Lists;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import com.xfd.entity.UserDescribe;

import java.time.Duration;

import static com.theokanning.openai.service.OpenAiService.buildApi;

public class GPTService {

    private OpenAiService service;

    public GPTService() {
        String token = System.getenv("openAiKey");
        service = new OpenAiService(buildApi(token, Duration.ofSeconds(60)));
    }

    public String doChat(UserDescribe userDescribe){
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setModel("gpt-3.5-turbo");
        chatCompletionRequest.setMaxTokens(2000);
        chatCompletionRequest.setN(1);
        ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), "你是远近小有名气的运势预测师");
        ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), "我叫郑成功,生日是1997年7月3日,是个厨师,身高180,体重70公斤," +
            "最近老板总是叫我加班,我十分厌烦,对手公司开价两倍挖角,如果我去的话,帮我预测一下接下来一年的运势将如何吧");
        chatCompletionRequest.setMessages(Lists.newArrayList(systemMessage, userMessage));
        ChatCompletionResult chatCompletionResult = service.createChatCompletion(chatCompletionRequest);
        return chatCompletionResult.getChoices().get(0).getMessage().getContent();

    }
}
