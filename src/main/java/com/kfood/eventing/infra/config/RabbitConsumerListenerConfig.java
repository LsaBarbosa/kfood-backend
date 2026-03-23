package com.kfood.eventing.infra.config;

import com.kfood.eventing.app.NonRetryableEventProcessingException;
import com.kfood.eventing.app.RetryableEventProcessingException;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.eventing.rabbit", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(EventConsumerProperties.class)
public class RabbitConsumerListenerConfig {

  @Bean
  public SimpleRabbitListenerContainerFactory internalEventRabbitListenerContainerFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory,
      MethodInterceptor internalEventRetryInterceptor) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setDefaultRequeueRejected(false);
    factory.setAdviceChain(internalEventRetryInterceptor);
    return factory;
  }

  @Bean
  public MethodInterceptor internalEventRetryInterceptor(
      RabbitTemplate rabbitTemplate, EventConsumerProperties properties) {
    var recoverer =
        new RepublishMessageRecoverer(rabbitTemplate, properties.dlxExchange())
            .errorRoutingKeyPrefix(properties.dlqRoutingPrefix());

    return RetryInterceptorBuilder.stateless()
        .configureRetryPolicy(
            retryPolicy ->
                retryPolicy
                    .maxRetries(properties.maxAttempts() - 1L)
                    .includes(RetryableEventProcessingException.class)
                    .excludes(
                        NonRetryableEventProcessingException.class,
                        AmqpRejectAndDontRequeueException.class))
        .backOffOptions(
            properties.initialIntervalMs(), properties.multiplier(), properties.maxIntervalMs())
        .recoverer(recoverer)
        .build();
  }
}
