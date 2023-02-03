package com.twitter.app.controller;

import com.twitter.app.model.Keywords;
import com.twitter.app.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("twitter")
public class TweetSearchController {

    private SearchService searchService;

    @Autowired
    public TweetSearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    //TODO: implement authentication here as we require user authentication to get tweeter secrets from repository.
    @PostMapping("search")
    public ResponseEntity searchTwitterForKeywords(Keywords keywords) {
        List<String> tweets = null;
        boolean tweetsStarted = searchService.searchTweetsOnTweeter(keywords);
        if (tweetsStarted) {
            tweets = searchService.getTweets(keywords);
        }
        return ResponseEntity.ok(tweets);
    }

    //TODO: what to do with this - we have to stop search on criteria.
    @RequestMapping("/search/by/stop")
    public ResponseEntity stopStream(Keywords keywords) {
        boolean stopped = false;
        log.info("stop request.");
        try {
            stopped = searchService.stopTweets();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        return ResponseEntity.ok(stopped);
    }

}
