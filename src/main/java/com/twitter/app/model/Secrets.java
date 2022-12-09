package com.twitter.app.model;

import lombok.Data;

@Data
public class Secrets {
    //{ID=104, USERID=244, CONSUMERKEY=testconsumekey, CONSUMERSECRET=testconsumersecrete, TOKEN=testtoken, SECRET=testsecrete}
    private int id;
    private int userId;
    private String consumerKey;
    private String consumerSecret;
    private String token;
    private String secret;
}
