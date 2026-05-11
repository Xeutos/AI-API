package org.example.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AiChat {

    private final ChatModel chatModel;
    private final List<Message> conversation = new ArrayList<>();
    private SystemMessage SystemPersonality;
    private static final Logger log = LoggerFactory.getLogger(AiChat.class);

    public AiChat(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    private String getChatCompletion(String message) {
        log.info("Sending prompt to AI model: '{}'", message);
        try {
            conversation.add(new UserMessage(message));
            return this.chatModel.call((Message) conversation);
        } catch (Exception e) {
            log.error("Error calling AI Model: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get completion: {}" + e.getMessage(), e);
        }
    }

    @Retryable
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest chatRequest, @RequestBody Personality personality) {
        if (chatRequest == null || chatRequest.message() == null || chatRequest.message().isBlank()) {
            log.warn("Received empty or null message.");
            return ResponseEntity.badRequest().body("Message cannot be empty.");
        }

        log.info("Processing chat request: '{}'", chatRequest.message());
        try {
            if (SystemPersonality == null) {
                SystemPersonality = new SystemMessage(personality.personality);
                conversation.add(SystemPersonality);
            }
                String response = getChatCompletion(chatRequest.message());
                return ResponseEntity.ok(response);
            } catch(Exception e){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Could not communicate with Ai service");
            }
        }

        public record ChatRequest(String message) {
        }

        public record Personality(String personality) {
        }
    }
