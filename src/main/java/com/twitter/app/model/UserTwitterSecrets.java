package com.twitter.app.model;

import lombok.Data;

@Data
public class UserTwitterSecrets {
    private String consumerKey;
    private String consumerSecret;
    private String  token;
    private String  secret;
}
