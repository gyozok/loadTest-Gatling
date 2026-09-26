package dev.gyozok.loadtest.orderconsumer.listener;

import dev.gyozok.loadtest.orderconsumer.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @KafkaListener(topics = "${app.kafka.topic.order-events}")
    public void handleOrderCreated(
            OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received OrderCreatedEvent: orderId={}, item={}, quantity={} (partition={}, offset={})",
                event.orderId(), event.item(), event.quantity(), partition, offset);
    }
}
