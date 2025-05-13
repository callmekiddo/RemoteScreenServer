package com.kiddo.remotescreen.server.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import com.kiddo.remotescreen.server.entity.Token;
import com.kiddo.remotescreen.server.model.TokenDto;
import com.kiddo.remotescreen.server.service.TokenService;

@Service
public class TokenServiceImpl implements TokenService {

    private final DynamoDBMapper dynamoDBMapper;

    public TokenServiceImpl(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    @Override
    public TokenDto getToken(String email, String token) {
        Token product = dynamoDBMapper.load(Token.class, email, token);
        return convertToDTO(product);
    }

    @Override
    public void lockToken(TokenDto tokenDto) {
        Token token = new Token();
        BeanUtils.copyProperties(tokenDto, token);
        dynamoDBMapper.save(token);

    }

    private TokenDto convertToDTO(Token token) {
        if (token == null) {
            return null;
        }
        return new TokenDto(token.getEmail(), token.getToken());
    }

}
