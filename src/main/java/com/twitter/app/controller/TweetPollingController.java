package com.twitter.app.controller;

import com.twitter.app.model.Keywords;
import com.twitter.app.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("twitter")
public class TweetPollingController {

    private SearchService searchService;

    @Autowired
    public TweetPollingController(SearchService searchService) {
        this.searchService = searchService;
    }

    @RequestMapping("poll")
    public ResponseEntity pollTweetsByKeywordsStream(Keywords keywords) {
        List<String> tweets = null;
        log.info("receive request for poll.");
        try {
            tweets = searchService.getTweets(keywords);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        return ResponseEntity.ok(tweets);
    }
}
