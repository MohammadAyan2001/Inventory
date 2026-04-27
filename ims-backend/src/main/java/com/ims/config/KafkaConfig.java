package com.ims.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Kafka topic and listener configuration.
 *
 * Partitions: 3 per topic — allows up to 3 parallel consumers per group.
 * Replication factor: 1 (dev); use 3 in production for fault tolerance.
 *
 * AckMode.RECORD: commit offset after each record is processed.
 * This ensures at-least-once delivery — consumers must be idempotent.
 */
@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.order-placed}")
    private String orderPlacedTopic;

    @Value("${kafka.topics.inventory-update}")
    private String inventoryUpdateTopic;

    @Value("${kafka.topics.order-failed}")
    private String orderFailedTopic;

    @Bean
    public NewTopic orderPlacedTopic() {
        return new NewTopic(orderPlacedTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic inventoryUpdateTopic() {
        return new NewTopic(inventoryUpdateTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic orderFailedTopic() {
        return new NewTopic(orderFailedTopic, 3, (short) 1);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
        ConsumerFactory<String, Object> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3); // 3 threads = 1 per partition
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
