package dev.gyozok.loadtest.orderconsumer.config;

import dev.gyozok.loadtest.orderconsumer.event.OrderCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    //Retry setups
    @Value("${app.kafka.retry.initial=interval=ms:1000}")
    private long retryInitialIntervalMs;

    @Value("${app.kafka.retry.max-interval-ms:10000}")
    private long retryMaxIntervalMs;

    @Value("${app.kafka.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${app.kafka.retry.max-elapsed-ms:30000}")
    private long retryMaxElapsedMs;

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderConsumerFactory(
            MeterRegistry meterRegistry
    ) {
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
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "dev.gyozok.orderconsumer.event");
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName());

        // order-producer's JsonSerializer sends a __TypeId__ header naming
        // its own class (dev.gyozok.orderservice.event.OrderCreatedEvent).
        // That class doesn't exist on order-consumer's classpath - these two
        // services deliberately don't share code (see OrderCreatedEvent's
        // own javadoc). Ignoring the header and trusting VALUE_DEFAULT_TYPE
        // instead means deserialization targets this service's own copy of
        // the record regardless of what the producer's class was named.
        config.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        DefaultKafkaConsumerFactory<String, OrderCreatedEvent> factory =
                new DefaultKafkaConsumerFactory<>(config);

        factory.addListener(new MicrometerConsumerListener<>(meterRegistry));

        return factory;
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
            ConsumerFactory<String, OrderCreatedEvent> orderConsumerFactory,
            DefaultErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }


    /**
     * Sends a message to <original-topic>-dlt (DeadLetterPublishingRecoverer's
     * default naming convention) after retries are exhausted, preserving the
     * original key and adding headers that record the exception and the
     * original topic/partition/offset - so a message on the DLT can be
     * traced back to exactly where and why it failed, rather than showing
     * up as an unexplained orphan.
     *
     * kafkaOperations is the dltKafkaTemplate bean from KafkaProducerConfig
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaOperations<String, OrderCreatedEvent> kafkaOperations
    ) {
        return new DeadLetterPublishingRecoverer(kafkaOperations);
    }

    /**
     * Retries a failing message with exponential backoff (1s, 2s, 4s, 8s...
     * capped at retryMaxIntervalMs) up to retryMaxElapsedMs total, then hands
     * it to the DeadLetterPublishingRecoverer instead of retrying forever.
     */
    @Bean
    public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff(retryInitialIntervalMs, retryMultiplier);
        backOff.setMaxInterval(retryMaxIntervalMs);
        backOff.setMaxElapsedTime(retryMaxElapsedMs);

        return new DefaultErrorHandler(deadLetterPublishingRecoverer, backOff);
    }
}
