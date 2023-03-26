package com.gpt.example;

import com.google.common.collect.Lists;
import com.gpt.Shit;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.service.OpenAiService;

import java.time.Duration;
import java.util.List;

import static com.theokanning.openai.service.OpenAiService.buildApi;

public class GPTExampleApplication {

    public static void main(String[] args) {
        try {

            String modelName = "gpt-3.5-turbo";
//            String token = "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";
            String token = "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";
            OpenAiService service = new OpenAiService(buildApi(token, Duration.ofSeconds(60)));
            List<Model> models = service.listModels();
            System.out.println(1);
            CompletionRequest completionRequest = new CompletionRequest();
            completionRequest.setModel("text-davinci-003");
            completionRequest.setPrompt("我叫郑成功,生日是1997年7月3日,是个厨师,身高180,体重70公斤," +
                "最近老板总是叫我加班,我十分厌烦,对手公司开价两倍挖角,如果我去的话,帮我预测一下接下来一年的运势将如何吧");
//            completionRequest.setSuffix("你好");
            completionRequest.setN(1);
//            completionRequest.setStop(Lists.newArrayList("stop"));
            completionRequest.setMaxTokens(2000);
            CompletionResult completionResult = service.createCompletion(completionRequest);
            System.out.println(completionResult.getChoices().get(0).getText());
            System.out.println(completionResult.getChoices().get(0).getFinish_reason());
            ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
            chatCompletionRequest.setModel("gpt-3.5-turbo");
            chatCompletionRequest.setMaxTokens(2000);
            chatCompletionRequest.setN(1);
            ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), "你是远近小有名气的运势预测师");
            ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), "我叫郑成功,生日是1997年7月3日,是个厨师,身高180,体重70公斤," +
                "最近老板总是叫我加班,我十分厌烦,对手公司开价两倍挖角,如果我去的话,帮我预测一下接下来一年的运势将如何吧");
            chatCompletionRequest.setMessages(Lists.newArrayList(systemMessage, userMessage));
            ChatCompletionResult chatCompletionResult = service.createChatCompletion(chatCompletionRequest);
            System.out.println(chatCompletionResult.getChoices().get(0).getMessage());
            System.out.println(chatCompletionResult.getChoices().get(0).getFinishReason());
        } catch (Exception e) {
            System.out.println(e.getClass());
        }
        System.out.println(1);

    }
}
