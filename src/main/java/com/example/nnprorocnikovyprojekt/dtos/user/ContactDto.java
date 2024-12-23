package com.example.nnprorocnikovyprojekt.dtos.user;

import java.util.List;

public class ContactDto {
    private String username;

    private String email;

    private PublicKeyDto publicKey;

    private Boolean alreadyAdded;

    private List<AuthorityDto> authorities;

    public ContactDto() {
    }

    public ContactDto(String username, String email, PublicKeyDto publicKey, Boolean alreadyAdded, List<AuthorityDto> authorities) {
        this.username = username;
        this.email = email;
        this.publicKey = publicKey;
        this.alreadyAdded = alreadyAdded;
        this.authorities = authorities;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public PublicKeyDto getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKeyDto publicKey) {
        this.publicKey = publicKey;
    }

    public Boolean getAlreadyAdded() {
        return alreadyAdded;
    }

    public void setAlreadyAdded(Boolean alreadyAdded) {
        this.alreadyAdded = alreadyAdded;
    }


    public List<AuthorityDto> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<AuthorityDto> authorities) {
        this.authorities = authorities;
    }
}
