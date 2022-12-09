package com.twitter.app.service;

import com.twitter.app.model.TwitterDetails;
import com.twitter.app.model.UserDetails;
import com.twitter.app.serviceimpl.TwitterClientServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TwitterClientService {

    Logger logger = LoggerFactory.getLogger(TwitterClientService.class);

    @Autowired
    private TwitterClientServiceImpl twitterClientServiceImpl;

    public int saveTwitterSecrets(TwitterDetails twitterDetails) {
        return twitterClientServiceImpl.saveTwitterSecrets(twitterDetails);
    }

    public int saveNewUser(UserDetails userDetails) {
        return twitterClientServiceImpl.saveNewUser(userDetails);
    }
}
