package com.example.nnprorocnikovyprojekt.controllers;

import com.example.nnprorocnikovyprojekt.dtos.conversation.MessageDto;
import com.example.nnprorocnikovyprojekt.entity.Conversation;
import com.example.nnprorocnikovyprojekt.entity.Message;
import com.example.nnprorocnikovyprojekt.entity.User;
import com.example.nnprorocnikovyprojekt.services.ConversationService;
import com.example.nnprorocnikovyprojekt.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private UserService userService;

    @Autowired
    private ConversationService conversationService;

    //https://medium.com/@poojithairosha/spring-boot-3-authenticate-websocket-connections-with-jwt-tokens-2b4ff60532b6
    //Asi chci destination variable a nepotrebuji hodnotu z Dto?
    @MessageMapping("/chat/{conversationId}")
    public ResponseEntity<String> chat(MessageDto messageDto, @DestinationVariable Integer conversationId) {
        User user = userService.getUserFromContext();

        if(user == null) return ResponseEntity.status(403).body("User not found");

        System.out.format("Message received: {%s}", messageDto.getMessage());
        try {
            //TODO forcyklus kde se odfiltruje ze seznamu prijemcu odesilatel
            String receiver = null;
            //Presunout do conversationService
            Conversation conversation = conversationService.getConversationById(conversationId);
            conversationService.sendMessageToAllSubscribersExceptUser(user, conversation, messageDto.getMessage());
        } catch (Exception e){
            return ResponseEntity.status(500).body("Failed to process message");
        }
        return ResponseEntity.status(200).body("Message processed, receivers notified");
    }

    /*@MessageMapping("/send/{conversationId}")
    @SendTo("/topic/messages/{conversationId}")
    public Message send(@DestinationVariable Integer conversationId, Message message) {
        return message;
    }*/
}