package dev.gyozok.loadtest.orderservice.config;

import dev.gyozok.loadtest.orderservice.event.OrderCreatedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import tools.jackson.databind.ser.jackson.JsonValueSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-server}")
    private String bootstrapServer;

    @Bean
    public KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate(ProducerFactory<String, OrderCreatedEvent> orderProducerFatory) {
        return new KafkaTemplate<>(orderProducerFatory);
    }

    @Bean
    public ProducerFactory<String, OrderCreatedEvent> orderProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        // Setup the entry point of the Kafka cluster
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);

        // Converts the key object into String
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        //Converts the value object into Json
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonValueSerializer.class);

        // wait for all insync replicas to acknowledge before considering the write successful.
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        // Retry transient failure (broker unavailable, leader election in progress)
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        // Prevent duplicate messages on retry, even the first was succeeded, but the ACK was lost
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(config);
    }
}
