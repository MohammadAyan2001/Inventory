package com.ims.kafka.consumer;

import com.ims.kafka.InventoryUpdateEvent;
import com.ims.kafka.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumers for IMS events.
 *
 * Consumer group "ims-group" ensures each message is processed by exactly
 * one instance when the app is horizontally scaled — Kafka distributes
 * partitions across group members.
 *
 * In a real microservices setup these would live in separate services
 * (e.g., notification-service, analytics-service). Here they demonstrate
 * the pattern within the monolith.
 */
@Slf4j
@Service
public class EventConsumer {

    @KafkaListener(
        topics = "${kafka.topics.order-placed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderPlaced(
        @Payload OrderPlacedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Consumed OrderPlacedEvent: order={}, status={}, partition={}, offset={}",
            event.getOrderReference(), event.getStatus(), partition, offset);

        // In production: trigger email notification, update analytics, etc.
        // Idempotency: check eventId in a processed-events store before acting
        processOrderEvent(event);
    }

    @KafkaListener(
        topics = "${kafka.topics.inventory-update}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeInventoryUpdate(
        @Payload InventoryUpdateEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Consumed InventoryUpdateEvent: product={}, type={}, qty={}->{}, partition={}, offset={}",
            event.getProductSku(), event.getUpdateType(),
            event.getPreviousQuantity(), event.getNewQuantity(), partition, offset);

        // In production: trigger reorder if newQuantity is below threshold
        processInventoryEvent(event);
    }

    private void processOrderEvent(OrderPlacedEvent event) {
        // Placeholder: send notification, update reporting DB, etc.
        log.debug("Processing order event for reference={}", event.getOrderReference());
    }

    private void processInventoryEvent(InventoryUpdateEvent event) {
        // Placeholder: trigger auto-reorder if stock is critically low
        log.debug("Processing inventory event for product={}", event.getProductSku());
    }
}
