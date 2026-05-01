package com.ims.kafka.consumer;

import com.ims.kafka.InventoryUpdateEvent;
import com.ims.kafka.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer — only active when app.kafka.enabled=true.
 * When Kafka is disabled (e.g. on Render free tier), this bean is not
 * created so no KafkaListenerContainerFactory is required.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
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
    }
}
