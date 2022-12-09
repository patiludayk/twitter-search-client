package com.twitter.app.model;

import com.twitter.hbc.core.Client;
import lombok.Builder;
import lombok.Data;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.concurrent.BlockingQueue;

@Data
@Builder
public class ClientTweets {
    private Client client;
    private BlockingQueue<String> msgQueue;
    private KafkaProducer<String, String> producer;
}
