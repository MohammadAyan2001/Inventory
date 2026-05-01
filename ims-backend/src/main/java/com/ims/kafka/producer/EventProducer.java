package com.ims.kafka.producer;

import com.ims.kafka.InventoryUpdateEvent;
import com.ims.kafka.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Provides an EventProducer bean.
 *
 * When app.kafka.enabled=true  → KafkaEventProducer (real Kafka publishing)
 * When app.kafka.enabled=false → NoOpEventProducer  (logs only, no Kafka dependency)
 *
 * This prevents the startup crash on Render where Kafka is not available.
 */
@Slf4j
@Configuration
public class EventProducer {

    // ── Real Kafka producer ────────────────────────────────────────────────────

    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
    public static class KafkaEventProducer extends EventProducer {

        private final KafkaTemplate<String, Object> kafkaTemplate;

        @Value("${kafka.topics.order-placed}")
        private String orderPlacedTopic;

        @Value("${kafka.topics.inventory-update}")
        private String inventoryUpdateTopic;

        public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        @Override
        public void publishOrderPlaced(OrderPlacedEvent event) {
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(orderPlacedTopic, event.getOrderReference(), event);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish OrderPlacedEvent for order={}: {}", event.getOrderReference(), ex.getMessage());
                } else {
                    log.debug("Published OrderPlacedEvent: order={}, partition={}, offset={}",
                        event.getOrderReference(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
        }

        @Override
        public void publishInventoryUpdate(InventoryUpdateEvent event) {
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(inventoryUpdateTopic, event.getProductId(), event);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish InventoryUpdateEvent for product={}: {}", event.getProductId(), ex.getMessage());
                } else {
                    log.debug("Published InventoryUpdateEvent: product={}, type={}", event.getProductId(), event.getUpdateType());
                }
            });
        }
    }

    // ── No-op producer (Kafka disabled) ───────────────────────────────────────

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public EventProducer noOpEventProducer() {
        return new EventProducer();
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
    public EventProducer kafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaEventProducer(kafkaTemplate);
    }

    // ── Default no-op methods (overridden by KafkaEventProducer) ──────────────

    public void publishOrderPlaced(OrderPlacedEvent event) {
        log.debug("Kafka disabled — skipping OrderPlacedEvent for order={}", event.getOrderReference());
    }

    public void publishInventoryUpdate(InventoryUpdateEvent event) {
        log.debug("Kafka disabled — skipping InventoryUpdateEvent for product={}", event.getProductId());
    }
}
