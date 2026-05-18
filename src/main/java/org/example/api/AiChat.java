package org.example.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1")
public class AiChat {

    private final ChatModel chatModel;

    private final Map<String, List<Message>> sessions = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(AiChat.class);

    public AiChat(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    private String getChatCompletion(String message, List<Message> conversation) {
        log.info("Sending prompt to AI model: '{}'", message);
        try {
            conversation.add(new UserMessage(message));

            Prompt prompt = new Prompt(conversation);

            ChatResponse response = chatModel.call(prompt);

            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Error calling AI Model: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get completion: {}" + e.getMessage(), e);
        }
    }

    @Retryable
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest chatRequest) {
        if (chatRequest == null || chatRequest.message() == null || chatRequest.message().isBlank()) {
            log.warn("Received empty or null message.");
            return ResponseEntity.badRequest().body("Message cannot be empty.");
        }
        try {
            List<Message> conversation =
                    sessions.computeIfAbsent(chatRequest.sessionId(),
                            id -> {
                                List<Message> msgs = new ArrayList<>();
                                msgs.add(new SystemMessage(chatRequest.personality()));
                                return msgs;
                            });
            conversation.add(new UserMessage(chatRequest.message()));

            String response = getChatCompletion(chatRequest.message(), conversation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not communicate with Ai service");
        }

    }

}
