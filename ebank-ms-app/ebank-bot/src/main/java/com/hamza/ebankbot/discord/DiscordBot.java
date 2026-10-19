package com.hamza.ebankbot.discord;

import com.hamza.ebankbot.agents.EbankAIAgent;
import org.springframework.messaging.core.MessageReceivingOperations;

@DiscordController
public class DiscordBot {
    private EbankAIAgent ebankAIAgent;
    public DiscordBot(EbankAIAgent ebankAIAgent) {
        this.ebankAIAgent = ebankAIAgent;
    }
    @DiscordMapping
    private void perform(MessageReceiveEvent event) {
        System.out.println(event);
        if(event.getAuthor().isBot()) return;
        String query = event.getMessage().getContentRaw();
        String response = ebankAIAgent.chat(new Prompt(query));
        event.getChannel().sendMessage(response).queue();
    }
}
