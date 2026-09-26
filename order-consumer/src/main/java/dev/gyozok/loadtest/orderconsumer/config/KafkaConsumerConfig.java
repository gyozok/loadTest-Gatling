package dev.gyozok.loadtest.orderconsumer.config;

import dev.gyozok.loadtest.orderconsumer.event.OrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Read from the beginning of the topic on first run (no committed
        // offset yet for this group) - matches auto-offset-reset in
        // application.yml, set here too since these Kafka client configs
        // are being built explicitly rather than left to autoconfiguration.
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Key deserializer is wrapped in ErrorHandlingDeserializer, same as
        // the value deserializer below - a malformed key would otherwise
        // crash the consumer thread just as easily as a malformed value.
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);

        // JsonDeserializer refuses to deserialize into any class it wasn't
        // explicitly told to trust, as a safety default - this whitelists
        // just the one package this consumer actually expects events from.
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.example.orderconsumer.event");
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName());

        // order-producer's JsonSerializer sends a __TypeId__ header naming
        // its own class (com.example.orderservice.event.OrderCreatedEvent).
        // That class doesn't exist on order-consumer's classpath - these two
        // services deliberately don't share code (see OrderCreatedEvent's
        // own javadoc). Ignoring the header and trusting VALUE_DEFAULT_TYPE
        // instead means deserialization targets this service's own copy of
        // the record regardless of what the producer's class was named.
        config.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * ErrorHandlingDeserializer means a single malformed message doesn't
     * crash the consumer thread - it's wrapped into a
     * DeserializationException that the container recognizes, logs, and
     * skips (via the container factory's default error handler), so the
     * rest of the partition keeps being processed. Without this, one bad
     * message would stop the whole consumer.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, OrderCreatedEvent> orderConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderConsumerFactory);
        return factory;
    }
}
