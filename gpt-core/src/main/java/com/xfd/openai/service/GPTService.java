package com.xfd.openai.service;

import com.google.common.collect.Lists;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import com.xfd.OpenAiException;
import com.xfd.openai.entity.UserDescribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;

import static com.theokanning.openai.service.OpenAiService.buildApi;

@Service
@Slf4j
public class GPTService {

    private OpenAiService service;

    @Value("${openAi.practice.model}")
    private String modelName;

    @Value("${openAi.practice.token}")
    private int tokenSize;

    @Value("${openAi.practice.prompt}")
    private String practicePrompt;

//    @PostConstruct
    private void initGPTService() {
        String token = System.getenv("openAiKey");
        service = new OpenAiService(buildApi(token, Duration.ofSeconds(60)));
    }

    public String practise(String userDescribe) {
        try {
            ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
            chatCompletionRequest.setModel(modelName);
            chatCompletionRequest.setMaxTokens(tokenSize);
            chatCompletionRequest.setN(1);
            ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), practicePrompt);
            ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), userDescribe);
            chatCompletionRequest.setMessages(Lists.newArrayList(systemMessage, userMessage));
            ChatCompletionResult chatCompletionResult = service.createChatCompletion(chatCompletionRequest);
            return chatCompletionResult.getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.error("openai_interface_error", e);
            throw new OpenAiException(e);
        }
    }


    @Value("${openAi.chat.model}")
    private String chatModel;

    @Value("${openAi.chat.token}")
    private int chatTokenSize;

    public ChatMessage chat(List<ChatMessage> chatContext) {
        try {
            ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
            chatCompletionRequest.setModel(chatModel);
            chatCompletionRequest.setMaxTokens(chatTokenSize);
            chatCompletionRequest.setN(1);
            chatCompletionRequest.setMessages(chatContext);
            ChatCompletionResult chatCompletionResult = service.createChatCompletion(chatCompletionRequest);
            return chatCompletionResult.getChoices().get(0).getMessage();
        } catch (Exception e) {
            log.error("openai_interface_error", e);
            throw new OpenAiException(e);
        }
    }

}
