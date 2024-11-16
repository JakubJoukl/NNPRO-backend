package com.example.nnprorocnikovyprojekt.dtos.conversation;

public class AddRemoveUserToConversationDto {
    private Integer conversationId;

    private String username;

    private String cipheredSymmetricKey;

    public Integer getConversationId() {
        return conversationId;
    }

    public void setConversationId(Integer conversationId) {
        this.conversationId = conversationId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCipheredSymmetricKey() {
        return cipheredSymmetricKey;
    }

    public void setCipheredSymmetricKey(String cipheredSymmetricKey) {
        this.cipheredSymmetricKey = cipheredSymmetricKey;
    }
}
