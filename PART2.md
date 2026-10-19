# Rapport de Projet - Systèmes Distribués - PART 01

**Référence :** https://www.youtube.com/watch?v=JHvxSZTWSsY
**Réalisé par :** HAMZA ELCADI - GLSID3

---

## 1 - Développer l'Agent AI basé sur Spring AI

On Créer un module spring boot `ebank-bot` :

![Création du module](screenShots/19.png)

Avec les dépendances suivantes :
- **Spring Web**
- **OpenAi**
- **Eureka Discovery Client**
- **Config Client**
- **Spring Boot Actuator** \

On configurer le fichier `application.properties`
```properties
spring.application.name=ebank-bot
spring.ai.openai.api-key=API_KEY
spring.ai.openai.chat.options.model=gpt-4o
spring.cloud.discovery.enabled=true
spring.cloud.config.enabled=false
server.port=8083
```
On creer `EbankChatBotController`
```java
@RestController
public class EbankChatbotController {
    private ChatClient chatClient;
    public EbankChatbotController(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }
    @GetMapping("/chat")
    public String chat(@RequestParam(name = "query", defaultValue = "Bonjour") String query){
        return chatClient.prompt(query).call().content();
    }
}
```
On utilisant la documetation swager : \
on ajoutons la dependance `Openapi`:
```xml
   <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>3.1.1</version>
   </dependency>
```

on lance `discovery-client` et `Ebank-bot` services, puis on entrere `http://localhost:8083/chat?query=hello`
![http://localhost:8083/chat?query=hello](screenShots/20.png) \
Note : la page n'est pas fonctioner a cause de cetter erreur : \
```text

2026-10-18T00:39:32.434+01:00 ERROR 17472 --- [ebank-bot] [nio-8083-exec-3] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] 
threw exception [Request processing failed: com.openai.errors.RateLimitException: 429: 
You have no credits remaining. Add credits to continue using the API at https://platform.openai.com/settings/organization/billing/.] with root cause
```
il dit que 'You have no credits remaining. Add credits to continue using the API'
donc on va utiliser `Gemini`
- 1 : on ajoutans la depandance
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-google-genai</artifactId>
</dependency>
```
- 2 : on change `application.properties`
```properties
spring.application.name=ebank-bot
#spring.ai.openai.api-key=${API_KEY}
#spring.ai.openai.chat.options.model=gpt-4o
spring.ai.google.genai.api-key=${GEMINI_API_KEY}
spring.ai.google.genai.chat.model=gemini-3.8-flash
spring.cloud.discovery.enabled=true
spring.cloud.config.enabled=false
server.port=8083
```
On le teste `http://localhost:8083/chat?query=hello` \
![http://localhost:8083/chat?query=hello](screenShots/21.png) \
on voir la documentation `swager` \
![http://localhost:8083/swagger-ui/index.html](screenShots/22.png) \
Note : llm est stateless c-a-d n'a pas de memory. \
On ajoutons une memory pour llm.
```java
public EbankChatbotController(ChatClient.Builder chatClient, ChatMemory chatMemory) {
    this.chatClient = chatClient
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
}
@GetMapping("/chat")
public String chat(
        @RequestParam(name = "query", defaultValue = "Bonjour") String query,
        @RequestParam(name = "conversationId", defaultValue = "default") String conversationId) {

    return chatClient.prompt()
            .user(query)
            .advisors(advisor -> advisor.param(
                    ChatMemory.CONVERSATION_ID, conversationId))
            .call()
            .content();
}
```
![http://localhost:8083/swagger-ui/index.html testing](screenShots/23.png) \
![http://localhost:8083/swagger-ui/index.html testing](screenShots/24.png) \
alors LLM a une memory maintenant car il memeroiser mon nom. \
Maintenant on creer une class `EbankAIAgent` dans la package `agents`.
```java
@Service
public class EbankAIAgent {
    private final ChatClient chatClient;

    public EbankAIAgent(
            ChatClient.Builder chatClient,
            ChatMemory chatMemory) {

        this.chatClient = chatClient
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
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
```
puis on changer `EbankChatbotController`
```java
@RestController
public class EbankChatbotController {

    EbankAIAgent ebankAIAgent;
    public EbankChatbotController(EbankAIAgent ebankAIAgent) {
        this.ebankAIAgent = ebankAIAgent;
    }


    @GetMapping("/chat")
    public String chat(
            @RequestParam(name = "query", defaultValue = "Bonjour") String query,
            @RequestParam(name = "conversationId", defaultValue = "default") String conversationId) {

        return ebankAIAgent.chat(query, conversationId);
    }
}
```

## 2 - Exposition les fonctionnalités des micro-services via MCP
On ajoutant `MCP` (model context protocle server) dans `customer-service` \
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```
On ajoutons dans `CustomerService` l'annotation `@MCPTool` ca make le service des tools qui l'agent ai peut ituliser.
```java
@Service
public class CustomerService {
    private CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }

    @McpTool(description = "Get all customers")
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @McpTool(description = "Find a customer by id")
    public Customer findCustomerById(@McpToolParam(description = "The customer id") Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer "+id+" not found"));
    }

    @McpTool(description = "Save new customer")
    public Customer saveCustomer(@McpToolParam(description = "The customer to save (name, email)") Customer customer){
        return customerRepository.save(customer);
    }
}
```
n'importe qu'il agent connais l'adresss de service peut acceder au methode via mcp. \
on ajoutons la configuration mcp dans le micro-service `customer-service`
```properties
spring.ai.mcp.server.protocol=streamable
```
`streamable` permet de faire une communication biderctionell entre l'agent et le micro-service.

## 3 - Tester les micro-services via MCP Client
On le tester via `Postman`.

## 4 - Connecter le chatbot avec les micro-servicew avec MCP
on ajoutans la dependace de `mcp-client` dans `ebank-bot` service
```xml
dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```
puis on configurer
```properties
spring.ai.mcp.client.streamable-http.connections.customers.url=http://localhost:8081/mcp
```
puis on ajoutans dans `EbankAIAgent` service la fonctionaliter de au demarage il va chercher les tools 
```java
public EbankAIAgent(
        ChatClient.Builder chatClient,
        ChatMemory chatMemory,
        ToolCallbackProvider tools) {

    this.chatClient = chatClient
            .defaultAdvisors(
                    MessageChatMemoryAdvisor.builder(chatMemory).build())
//                .defaultToolCallbacks(tools)
            .defaultTools(tools)
            .build();
}
```
On le teste, on le demande liste des clients :
![http://localhost:8083/swagger-ui/index.html testing avec liste des clients](screenShots/25.png) \

On le demande de `nom de client numero 1`
![http://localhost:8083/swagger-ui/index.html testing avec save un client](screenShots/26.png) \

Maintenant on va deliminer et limiter chatbot pour juste repondre au question concerner les client el les comptes \
On ajoutons `defaultSystem` dans `EbankAIAgent`
```java
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
```
On le test avec la prompt `je veux manger`
![http://localhost:8083/swagger-ui/index.html testing avec prompt hors context](screenShots/27.png) \

On fait la meme chose avec `Ebank-service` \
On ajoutons le depandence `mcp`
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```
Puis on ajoutons les annotations de `MCP` dans `EbankService`
```java
@Service
public class EbankService {
    BankAccountRepository bankAccountRepository;
    CustomerRestClient customerRestClient;
    public EbankService(BankAccountRepository bankAccountRepository, CustomerRestClient customerRestClient) {
        this.bankAccountRepository = bankAccountRepository;
        this.customerRestClient = customerRestClient;

    }

    @McpTool(description = "Get all Bank accounts")
    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    @McpTool(description = "Get Bank Account by id")
    public BankAccount getBankAccountById(@McpToolParam(description = "The bank account id") String id) {
        BankAccount bankAccount =  bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("bankAccount not found"));
        bankAccount.setCustomer(customerRestClient.getCustomerById(bankAccount.getCustomerId()));
        return bankAccount;
    }

    @McpTool(description = "save a customer")
    public BankAccount saveBankAccount(@McpToolParam(description = "The bank account to save ()") BankAccount bankAccount) {
        try {
            Customer customer = customerRestClient.getCustomerById(bankAccount.getCustomerId());
            bankAccount.setId(UUID.randomUUID().toString());
            bankAccount.setCreatedAt(new Date());
            return bankAccountRepository.save(bankAccount);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());

        }
    }
}
```
et on ajoutons mcp dans `application.properties`
```properties
spring.ai.mcp.client.streamable-http.connections.ebank.url=http://localhost:8082/mcp
```
Remarque : Comme la clé API fournie sur Classroom a été publiée sur GitHub et qu’elle n’est désormais plus fonctionnelle, et que les limites d’utilisation de Gemini sont rapidement atteintes, 
je ne peux pas fournir de captures d’écran illustrant le fonctionnement du chatbot.

on rendre l'affichage de reponse de chatbot, \
dans `EbankChatbotController`
```java
    @GetMapping("/chat", produces = MediaType.TEXT_PLAIN_VALUE)
```

[//]: # (output using the gateway to get clients and bank accounts)
## 5 - Tester le chatbot avec un client Discord
On ajoutons la dependace de `discord` dans `ebank-bot`
```xml
<dependency>
    <groupId>com.zgamelogic</groupId>
    <artifactId>spring-boot-strater-discord</artifactId>
    <version>5.0.4</version>
</dependency>
```
on creer la class `DiscordBot`
```java
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
```
et on change `application.properties`
```properties
discord.token=${DISCORD_TOKEN}
```
pour avoir `DISCORD_TOKEN` on accceder `https://discord.com/developers/applications?new_application=true` \
on creer une application \
![creationo d'une application discord](screenShots/28.png) \
puis on entrer la fentere `bot` et puis `reset token` \
![reset token pour le bot](screenShots/29.png) \
puis on activer les droits :
![les droits de bot](screenShots/30.png) \
puis on creer un serveur discord \
![serveur discord](screenShots/31.png) \
puis on entrer l'instalation tab dans `https://discord.com/developers/applications/`
et on copie le lien d'application, et on ajouter le bot a un serveur. \
![serveur discord](screenShots/32.png) \




## 6 - Tester le chatbot avec un Client Telegram
On ajoutons la depandance de `telegram` dans `ebank-bot`
```xml
<dependency>
    <groupId>org.telegram</groupId>
    <artifactId>telegrambots-spring-boot-strater</artifactId>
    <version>6.9.7.1</version>
</dependency>
```
apres on creer un bot utilisant `bot-father`
![bot father conversation](screenShots/33.png) \
et on ajoutons dans `application.properties`
```properties
telegram.token=${TELEGRAM_TOKEN}
```
Et dans la class `telegram.TelegramBot`
```java
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
}
```
on update `query` from string to prompt
dans `EbankChatbotController` \
```java
@GetMapping("/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(
            @RequestParam(name = "query", defaultValue = "Bonjour") String query,
            @RequestParam(name = "conversationId", defaultValue = "default") String conversationId) {

        return ebankAIAgent.chat(new Prompt(query), conversationId);
    }
```
dans `DiscordBot.java`
```java
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
```
puis on resoudere le conflit entre `telegram` et `discovery-service` \
dans le fichier `application.properties` on ajoutons
```properties
eureka.client.jersey.enabled=flase
```
le discovery-service  a creer ce bean, c'est parce que on n'a pas besoin de creer dans telegram.

[//]: # (image de tesing le bot utilisant telegram)