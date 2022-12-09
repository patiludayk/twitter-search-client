package com.twitter.app.service;

import com.twitter.app.model.Keywords;
import com.twitter.app.serviceimpl.SearchServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@Slf4j
public class SearchService {

    private SearchServiceImpl searchServiceImpl;

    @Autowired
    public SearchService(SearchServiceImpl searchServiceImpl) {
        this.searchServiceImpl = searchServiceImpl;
    }

    public boolean searchTweetsOnTweeter(Keywords keywords) {
        //Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);
        return searchServiceImpl.createClientAndStartProducingTweetsToTopic(keywords, msgQueue);

    }

    public List<String> getTweets(Keywords keywords) {
        log.info("polling for keywords..");
        return searchServiceImpl.getTweetsForKeywords(keywords);
    }

    public boolean stopTweets() {
        log.info("stop polling for.");
        return searchServiceImpl.stopClient();
    }
}
