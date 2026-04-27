package com.ims.kafka.producer;

import com.ims.kafka.InventoryUpdateEvent;
import com.ims.kafka.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for order and inventory events.
 *
 * Partitioning strategy:
 * - orderReference is used as the message key for order events.
 *   This ensures all events for the same order land on the same partition,
 *   preserving ordering per order.
 * - productId is used as key for inventory events for the same reason.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-placed}")
    private String orderPlacedTopic;

    @Value("${kafka.topics.inventory-update}")
    private String inventoryUpdateTopic;

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

    public void publishInventoryUpdate(InventoryUpdateEvent event) {
        String key = String.valueOf(event.getProductId());
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(inventoryUpdateTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish InventoryUpdateEvent for product={}: {}", event.getProductId(), ex.getMessage());
            } else {
                log.debug("Published InventoryUpdateEvent: product={}, type={}", event.getProductId(), event.getUpdateType());
            }
        });
    }
}
