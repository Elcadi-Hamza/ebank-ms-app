package com.hamza.ebankbot.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class EbankAIAgent {
    private ChatClient chatClient;

    public EbankAIAgent(
            ChatClient.Builder chatClient,
            ChatMemory chatMemory,
            ToolCallbackProvider tools) {

        this.chatClient = chatClient
                .defaultSystem("""
                        Vous un assistant qui se charge de repondre aux question
                        de l'utilsateur a propos des clients et des compte bancaires, en fonction du contexte fourni.
                        Si aucun contexte n'est fourni, repond avec JE NE SAIT PAS.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
//                .defaultToolCallbacks(tools)
                .defaultTools(tools)
                .build();
    }
    public String chat(String query, String conversationId) {

        return chatClient.prompt()
                .user(query)
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
