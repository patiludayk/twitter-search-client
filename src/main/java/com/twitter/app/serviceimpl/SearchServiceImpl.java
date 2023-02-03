package com.twitter.app.serviceimpl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import com.twitter.app.model.ClientTweets;
import com.twitter.app.model.Keywords;
import com.twitter.app.model.Secrets;
import com.twitter.app.repository.TwitterRepository;
import com.twitter.app.utils.TwitterClientProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SearchServiceImpl {

    private TwitterRepository twitterRepository;

    private TwitterClientProvider twitterClientProvider;

    private ObjectMapper objectMapper;

    private KafkaTemplate<String, String> kafkaTemplate;

    @Value(value = "${twitter.kafka.topic:twitter}")
    private String twitterTopic;

    @Autowired
    public SearchServiceImpl(TwitterRepository twitterRepository, TwitterClientProvider twitterClientProvider, KafkaTemplate<String, String> kafkaTemplate) {
        this.twitterRepository = twitterRepository;
        this.twitterClientProvider = twitterClientProvider;
        this.kafkaTemplate = kafkaTemplate;
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    }

    public boolean createTwitterClientAndGetTweets(Keywords keywords, BlockingQueue<String> msgQueue) {
        Secrets secrets = getUserSecrets(keywords);

        // keywords to search
        List<String> terms = getTerms(keywords.getKeywords());

        // make sure you close same client otherwise there will be resource leak
        ClientTweets client = twitterClientProvider.getClient(secrets, terms);

        //client.setProducer(getProducerForUserTweets());

        client.getClient().connect();

        return startProducingTweetsToTopic(client);
    }

    private Secrets getUserSecrets(Keywords keywords) {
        //TODO: not good have authentication inplace get user details from there.
        //1. call database service using feign client to get secrets based on username
        Map<String, Object> userSecrets = twitterRepository.findUserSecrets(keywords.getUser());
        Secrets secrets = objectMapper.convertValue(userSecrets, Secrets.class);
        return secrets;
    }

    public List<String> getTweetsForKeywords(Keywords keywords) {
        // Kafka Consumer
        KafkaConsumer<String, String> consumer = createConsumer("topic-tweets");
        List<String> tweets = getTweets(consumer);

        log.info("total tweets polled: " + tweets.size());

        return tweets;
    }

    public boolean stopClient() {
        return twitterClientProvider.closeClientAfter();
    }

    private boolean startProducingTweetsToTopic(ClientTweets client) {
        while (!client.getClient().isDone()) {
            String msg = null;
            try {
                msg = client.getMsgQueue().poll(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Something went wrong...");
                client.getClient().stop();
                return false;
            }
            if (msg != null) {
                kafkaTemplate.send(twitterTopic, msg);
            }
        }
        return true;
    }

/*    private KafkaProducer<String, String> getProducerForUserTweets() {

        String bootstrapServers = "localhost:9092";

        Properties producerProps = new Properties();
        // producer properties
        producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create safe producer
        producerProps.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producerProps.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        producerProps.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        // high throughput producer
        producerProps.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        producerProps.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
        producerProps.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024));// 32KB

        // create producer
        return new KafkaProducer<String, String>(producerProps);
    }*/

    public static KafkaConsumer<String, String> createConsumer(String topic) {

        String bootstrapServers = "localhost:9092";
        String groupId = "kafka-demo-twitter";

        // create consumer configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // disable auto commit of offsets
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100"); // disable auto commit of offsets

        // create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);
        consumer.subscribe(Arrays.asList(topic));

        return consumer;
    }

    private List<String> getTweets(KafkaConsumer<String, String> consumer) {
        // loop to send tweets to kafka - we will not do that as we poll based on records count
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100)); // new in Kafka 2.0.0

        Integer recordCount = records.count();
        log.info("Received " + recordCount + " records");

        List<String> tweets = new ArrayList<>();

        for (ConsumerRecord<String, String> record : records) {
            try {
                String id = extractIdFromTweet(record.value());
                log.info("id: " + id + ", tweet: " + record.value());

                tweets.add(record.value());
                log.info("Tweet id: " + id);
            } catch (NullPointerException e) {
                log.warn("skipping bad data: " + record.value());
            }
        }
        if (recordCount > 0) {
            log.info("Committing offsets...");
            consumer.commitSync();
            log.info("Offsets have been committed");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("tweets at real time: " + tweets);
        consumer.close();
        return tweets;
    }

    private List<String> getTerms(String keywords) {
        return Arrays.stream(keywords.split(",")).map(String::trim).collect(Collectors.toList());
    }

    private static String extractIdFromTweet(String tweetJson) {
        JsonParser jsonParser = new JsonParser();
        // gson library
        return jsonParser.parse(tweetJson).getAsJsonObject().get("id_str").getAsString();
    }
}
