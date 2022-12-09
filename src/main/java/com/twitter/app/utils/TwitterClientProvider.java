package com.twitter.app.utils;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.twitter.app.model.ClientTweets;
import com.twitter.app.model.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

@Component
@Scope("prototype")
public class TwitterClientProvider {

    Logger logger = LoggerFactory.getLogger(TwitterClientProvider.class);

    private ClientTweets client;

    public ClientTweets getClient (Secrets secrets, List<String> terms) {
        /**
         * Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream
         */
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);

        // create twitter client
        client = createTwitterClient(msgQueue, secrets, terms);
        return client;
    }

    public boolean closeClientAfter() {
        // adding shutdown hook for graceful shutdown
        try {
            Runtime.getRuntime().addShutdownHook(new Thread( () -> {
                logger.info("stopping application...");
                logger.info("shutting down twitter client...");
                client.getClient().stop();
                client.getMsgQueue().clear();
                logger.info("done.");
            }));
            return true;
        }
        catch (Exception e) {
            logger.error("error stopping client." + e.getMessage());
            return false;
        }
    }

    private ClientTweets createTwitterClient (BlockingQueue<String> msgQueue, Secrets secrets, List<String> terms) {

        /**
         * Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth)
         */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        // Optional: set up some followings and track terms
        // List<Long> followings = Lists.newArrayList(1234L, 566788L);
        // List<String> terms = Lists.newArrayList("modi");
        // hosebirdEndpoint.followings(followings);
        hosebirdEndpoint.trackTerms(terms); // keyword based search

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(secrets.getConsumerKey(),
                                                 secrets.getConsumerSecret(),
                                                 secrets.getToken(),
                                                 secrets.getSecret());

        ClientBuilder builder = new ClientBuilder().name("Hosebird-Client-01") // optional: mainly for the logs
                                                   .hosts(hosebirdHosts)
                                                   .authentication(hosebirdAuth)
                                                   .endpoint(hosebirdEndpoint)
                                                   .processor(new StringDelimitedProcessor(msgQueue));
        // .eventMessageQueue(eventQueue); // optional: use this if you want to process client
        // events
        return ClientTweets.builder().client(builder.build()).msgQueue(msgQueue).build();
    }

}
