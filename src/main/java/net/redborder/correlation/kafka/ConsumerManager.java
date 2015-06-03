package net.redborder.correlation.kafka;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import net.redborder.correlation.util.ConfigFile;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Date: 3/6/15 18:16.
 */
public class ConsumerManager {
    private static ConsumerConnector consumer;
    private Map<String, ExecutorService> executors;


    public ConsumerManager() {
        Properties props = new Properties();
        props.put("auto.commit.enable", "true");
        props.put("zookeeper.connect", ConfigFile.getZkConnect());
        props.put("group.id", "rb-correlation-engine");
        props.put("zookeeper.session.timeout.ms", "400");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "60000");
        props.put("auto.offset.reset", "largest");
        consumer = kafka.consumer.Consumer.createJavaConsumerConnector(new ConsumerConfig(props));
        executors = new LinkedHashMap<>();
    }


    public void start(List<Topic> topics) {
        Map<String, Integer> topicsHash = new HashMap<>();

        for(Topic topic : topics){
            topicsHash.putAll(topic.toMap());
        }

        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicsHash);

        for(Topic topic : topics){
            List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic.getName());
            ExecutorService executor = Executors.newFixedThreadPool(topic.getPartitions());

            for (final KafkaStream stream : streams) {
                executor.submit(new Consumer(stream, topic.getParser()));
            }

            executors.put(topic.getName(), executor);
        }
    }

    public void shutdown() {
        if (consumer != null) consumer.shutdown();
        for (ExecutorService executor : executors.values())
            if (executor != null) executor.shutdown();
    }

}
