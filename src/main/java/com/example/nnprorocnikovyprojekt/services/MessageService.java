package com.example.nnprorocnikovyprojekt.services;

import com.example.nnprorocnikovyprojekt.entity.Message;
import com.example.nnprorocnikovyprojekt.repositories.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(Message message){
        message.getConversation().getMessages().add(message);
        messageRepository.save(message);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Message message){
        messageRepository.delete(message);
    }

    @Transactional(rollbackFor = Exception.class)
    public Message getMessageById(Integer messageId) {
        return messageRepository.getMessageByMessageId(messageId);
    }


    @Transactional(rollbackFor = Exception.class)
    public void deleteExpiredMessages() {
        List<Message> messages = messageRepository.getMessagesByValidToBefore(Instant.now());
        messageRepository.deleteAll(messages);
    }
}
