package com.example.nnprorocnikovyprojekt.dtos.conversation;

public class AddRemoveUserToConversationDto {
    private Integer conversationId;

    private CipheredSymmetricKeysDto user;

    public Integer getConversationId() {
        return conversationId;
    }

    public void setConversationId(Integer conversationId) {
        this.conversationId = conversationId;
    }

    public CipheredSymmetricKeysDto getUser() {
        return user;
    }

    public void setUser(CipheredSymmetricKeysDto user) {
        this.user = user;
    }
}
