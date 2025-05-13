package com.kiddo.remotescreen.server.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@DynamoDBTable(tableName = "users")
public class User implements UserDetails {

    @DynamoDBHashKey(attributeName = "email")
    private String email;

    @DynamoDBAttribute
    private String fullName;

    @DynamoDBAttribute
    private String password;

    @DynamoDBAttribute
    private Integer secret;

    @DynamoDBAttribute
    private String action;


    @DynamoDBIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    @DynamoDBIgnore
    public String getUsername() {
        return email;
    }

    @Override
    @DynamoDBIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @DynamoDBIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @DynamoDBIgnore
    public boolean isEnabled() {
        return true;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Integer getSecret() {
        return secret;
    }

    public String getAction() {
        return action;
    }

    public void setSecret(Integer secret) {
        this.secret = secret;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
