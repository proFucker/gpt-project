package com.gpt.example;

import com.gpt.Shit;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.service.OpenAiService;

import java.util.List;

public class GPTExampleApplication {

    public static void main(String[] args) {
        try {

            String token = "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";
            OpenAiService service = new OpenAiService(token);
            List<Model> models = service.listModels();
            System.out.println(1);
        }catch (Exception e){
            System.out.println(1);
        }
        System.out.println(1);

    }
}
