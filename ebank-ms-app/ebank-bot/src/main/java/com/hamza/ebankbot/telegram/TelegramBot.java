package com.hamza.ebankbot.telegram;

import com.hamza.ebankbot.agents.EbankAIAgent;
import jakarta.annotation.PostConstruct;
import lombok.Value;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Value("${telegram.token}")
    private String telgramBotToken;
    private EbankAIAgent aiAgent;
    public TelegramBot(EbankAIAgent aiAgent) {
        this. aiAgent = aiAgent;
    }
    @PostConstruct
    public void registerTelegramBot() {
        try{
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.regiserBot(this);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void onUpdateReceived(Update telegramRequest) {
        try {
            if(!telegramRequest.hasMessage()) return;
            String messageText = telegramRequest.getMessage().getText();
            Long chatId = telegramRequest.getMessage().getChatId();
            List<PhotoSize> photos = telegramRequest.getMessage().getPhoto();
            List<Media> mediaList = new ArrayList<>();
            String caption = null;
            if(photos != null) {
                caption = telegramRequest.getMessage().getCaption();
                if(caption == null) caption = "What do you see in this image";
                for(PhotoSize ps : photos) {
                    String fileId = ps.getFileId();
                    GetFile getFile = new GetFile();
                    getFile.setFileId(fileId);
                    File file = execute(getFile);
                    String filePath = file.getFilePath();
                    String textUrl = "https://api.telegram.org/file/bot"
                            +getBotToken()+"/"+filePath;
                    URL fileUrl = new URL(textUrl);
                    mediaList.add(Media.builder()
                            .id(fileId)
                            .mimeType(MimeTypeUtils.IMAGE_PNG)
                            .data(new UrlResource(fileUrl))
                            .build());
                }
            }
            String query = messageText != null?messageText:caption;
            UserMessage userMessage = UserMessage.builder()
                    .text(query)
                    .media(mediaList)
                    .build();
            sendTypingQuestion(chatId);
            String answer = aiAgent.chat(new Prompt(userMessage));
            sendTextMessage(chatId, answer);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername(){return "ebank_137_bot";}

    @Override
    public String getBotToken() {return telgramBotToken;}

    public void sendTextMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
        execute(sendMessage);
    }

    private void sendTypingQuestion(Long chatId)throws TelegramApiException{
        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(String.valueOf(chatId));
        sendChatAction.setAction(ActionType.TYPING);
        execute(sendChatAction);

    }
}
