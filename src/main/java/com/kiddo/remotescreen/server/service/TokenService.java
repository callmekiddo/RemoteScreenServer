package com.kiddo.remotescreen.server.service;


import com.kiddo.remotescreen.server.model.TokenDto;

public interface TokenService {

    TokenDto getToken(String email, String token);
    void lockToken(TokenDto tokenDto);

}
