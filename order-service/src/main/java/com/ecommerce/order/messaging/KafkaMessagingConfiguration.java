package com.ecommerce.order.messaging;

import com.ecommerce.events.inventory.InventoryTopics;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class KafkaMessagingConfiguration {

    @Bean
    NewTopic inventoryCommandsTopic() {
        return TopicBuilder.name(InventoryTopics.COMMANDS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic inventoryResultsTopic() {
        return TopicBuilder.name(InventoryTopics.RESULTS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic inventoryResultsDeadLetterTopic() {
        return TopicBuilder.name(InventoryTopics.RESULTS + ".dlt")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        record.topic() + ".dlt", record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
    }
}
