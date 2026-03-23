package com.kfood.eventing.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.KfoodBackendApplication;
import com.kfood.eventing.app.ConsumedEventMetadata;
import com.kfood.eventing.app.EventEnvelope;
import com.kfood.eventing.app.IdempotentEventConsumerExecutor;
import com.kfood.eventing.app.NonRetryableEventProcessingException;
import com.kfood.eventing.app.PaymentConfirmedPayload;
import com.kfood.eventing.app.RetryableEventProcessingException;
import com.kfood.eventing.infra.persistence.ProcessedEventRepository;
import com.kfood.support.PostgreSqlContainerIT;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RabbitConsumerRetryAndDlqIT extends PostgreSqlContainerIT {

  @Container
  static final RabbitMQContainer RABBITMQ_CONTAINER =
      new RabbitMQContainer("rabbitmq:3.13-management");

  @AfterEach
  void tearDown() {
    try (var context = applicationContext()) {
      context.getBean(ProcessedEventRepository.class).deleteAll();
      context.getBean(FailingPaymentConfirmedHandler.class).reset();
      purgeQueue(context, "kfood.payment.confirmed.q");
      purgeQueue(context, "kfood.payment.confirmed.q.dlq");
    }
  }

  @Test
  void shouldRetryTemporaryFailureAndApplySideEffectOnlyOnce() throws Exception {
    try (var context = applicationContext()) {
      var handler = context.getBean(FailingPaymentConfirmedHandler.class);
      var rabbitTemplate = context.getBean(RabbitTemplate.class);
      var processedEventRepository = context.getBean(ProcessedEventRepository.class);
      var amqpAdmin = context.getBean(AmqpAdmin.class);

      handler.failTwiceThenSucceed();
      publishPaymentConfirmed(
          rabbitTemplate, context.getBean(ObjectMapper.class), UUID.randomUUID());

      Awaitility.await()
          .untilAsserted(
              () -> {
                assertThat(handler.attempts()).isEqualTo(3);
                assertThat(handler.sideEffects()).isEqualTo(1);
                assertThat(processedEventRepository.count()).isEqualTo(1);
                assertThat(queueDepth(amqpAdmin, "kfood.payment.confirmed.q.dlq")).isZero();
              });
    }
  }

  @Test
  void shouldSendMessageToDlqOnNonRetryableFailure() throws Exception {
    try (var context = applicationContext()) {
      var handler = context.getBean(FailingPaymentConfirmedHandler.class);
      var rabbitTemplate = context.getBean(RabbitTemplate.class);
      var processedEventRepository = context.getBean(ProcessedEventRepository.class);
      var amqpAdmin = context.getBean(AmqpAdmin.class);

      handler.failAlwaysNonRetryable();
      publishPaymentConfirmed(
          rabbitTemplate, context.getBean(ObjectMapper.class), UUID.randomUUID());

      Awaitility.await()
          .untilAsserted(
              () -> {
                assertThat(handler.attempts()).isEqualTo(1);
                assertThat(handler.sideEffects()).isZero();
                assertThat(processedEventRepository.count()).isZero();
                assertThat(queueDepth(amqpAdmin, "kfood.payment.confirmed.q.dlq")).isEqualTo(1);
              });
    }
  }

  private ConfigurableApplicationContext applicationContext() {
    return new SpringApplicationBuilder(
            KfoodBackendApplication.class, TestConsumerConfiguration.class)
        .web(WebApplicationType.SERVLET)
        .properties(properties())
        .run();
  }

  private Map<String, Object> properties() {
    return Map.ofEntries(
        Map.entry("spring.rabbitmq.host", RABBITMQ_CONTAINER.getHost()),
        Map.entry("spring.rabbitmq.port", RABBITMQ_CONTAINER.getAmqpPort()),
        Map.entry("spring.rabbitmq.username", RABBITMQ_CONTAINER.getAdminUsername()),
        Map.entry("spring.rabbitmq.password", RABBITMQ_CONTAINER.getAdminPassword()),
        Map.entry("spring.flyway.enabled", true),
        Map.entry("spring.jpa.hibernate.ddl-auto", "validate"),
        Map.entry("app.security.jwt-secret", "12345678901234567890123456789012"),
        Map.entry("app.security.jwt-expiration-seconds", 3600),
        Map.entry("app.eventing.rabbit.enabled", true),
        Map.entry("app.eventing.rabbit.startup-verify", false),
        Map.entry("app.eventing.rabbit.exchange", "kfood.events"),
        Map.entry("app.eventing.rabbit.consumer.max-attempts", 3),
        Map.entry("app.eventing.rabbit.consumer.initial-interval-ms", 100),
        Map.entry("app.eventing.rabbit.consumer.multiplier", 2.0),
        Map.entry("app.eventing.rabbit.consumer.max-interval-ms", 1000),
        Map.entry("app.eventing.rabbit.consumer.dlx-exchange", "kfood.events.dlx"),
        Map.entry("app.eventing.rabbit.consumer.dlq-routing-prefix", "dlq."),
        Map.entry("app.eventing.rabbit.order-created.queue", "kfood.order.created.q"),
        Map.entry("app.eventing.rabbit.order-created.routing-key", "order.created"),
        Map.entry("app.eventing.rabbit.order-status-changed.queue", "kfood.order.status.changed.q"),
        Map.entry("app.eventing.rabbit.order-status-changed.routing-key", "order.status.changed"),
        Map.entry("app.eventing.rabbit.payment-confirmed.queue", "kfood.payment.confirmed.q"),
        Map.entry("app.eventing.rabbit.payment-confirmed.routing-key", "payment.confirmed"));
  }

  private void publishPaymentConfirmed(
      RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, UUID eventId)
      throws JsonProcessingException {
    var envelope =
        new EventEnvelope<>(
            eventId,
            "payment.confirmed",
            1,
            Instant.now().toString(),
            "tenant-1",
            "corr-1",
            new PaymentConfirmedPayload("pay-1", "ord-1", "MOCK_PSP", new BigDecimal("57.90")));

    rabbitTemplate.convertAndSend(
        "kfood.events",
        "payment.confirmed",
        objectMapper.writeValueAsString(envelope),
        message -> {
          message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
          return message;
        });
  }

  private int queueDepth(AmqpAdmin amqpAdmin, String queueName) {
    var queueProperties = amqpAdmin.getQueueProperties(queueName);
    if (queueProperties == null) {
      return 0;
    }

    return ((Number) queueProperties.getOrDefault("QUEUE_MESSAGE_COUNT", 0)).intValue();
  }

  private void purgeQueue(ConfigurableApplicationContext context, String queueName) {
    context.getBean(RabbitTemplate.class).execute(channel -> channel.queuePurge(queueName));
  }

  @TestConfiguration
  static class TestConsumerConfiguration {

    @Bean
    FailingPaymentConfirmedHandler failingPaymentConfirmedHandler() {
      return new FailingPaymentConfirmedHandler();
    }

    @Bean
    TestPaymentConfirmedConsumer testPaymentConfirmedConsumer(
        ObjectMapper objectMapper,
        IdempotentEventConsumerExecutor idempotentEventConsumerExecutor,
        FailingPaymentConfirmedHandler failingPaymentConfirmedHandler) {
      return new TestPaymentConfirmedConsumer(
          objectMapper, idempotentEventConsumerExecutor, failingPaymentConfirmedHandler);
    }
  }

  static class TestPaymentConfirmedConsumer {

    private final ObjectMapper objectMapper;
    private final IdempotentEventConsumerExecutor idempotentEventConsumerExecutor;
    private final FailingPaymentConfirmedHandler failingPaymentConfirmedHandler;

    TestPaymentConfirmedConsumer(
        ObjectMapper objectMapper,
        IdempotentEventConsumerExecutor idempotentEventConsumerExecutor,
        FailingPaymentConfirmedHandler failingPaymentConfirmedHandler) {
      this.objectMapper = objectMapper;
      this.idempotentEventConsumerExecutor = idempotentEventConsumerExecutor;
      this.failingPaymentConfirmedHandler = failingPaymentConfirmedHandler;
    }

    @RabbitListener(
        queues = "#{@paymentConfirmedQueue.name}",
        containerFactory = "internalEventRabbitListenerContainerFactory")
    void onMessage(String body) throws JsonProcessingException {
      var event =
          objectMapper.readValue(
              body, new TypeReference<EventEnvelope<PaymentConfirmedPayload>>() {});

      idempotentEventConsumerExecutor.execute(
          "test-payment-confirmed-consumer",
          new ConsumedEventMetadata(
              event.eventId(), event.eventType(), event.payload().paymentId()),
          () -> failingPaymentConfirmedHandler.handle(event));
    }
  }

  static class FailingPaymentConfirmedHandler {

    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicInteger sideEffects = new AtomicInteger();
    private volatile FailureMode failureMode = FailureMode.FAIL_TWICE_THEN_SUCCEED;

    void failTwiceThenSucceed() {
      failureMode = FailureMode.FAIL_TWICE_THEN_SUCCEED;
      reset();
    }

    void failAlwaysNonRetryable() {
      failureMode = FailureMode.FAIL_ALWAYS_NON_RETRYABLE;
      reset();
    }

    void handle(EventEnvelope<PaymentConfirmedPayload> event) {
      var currentAttempt = attempts.incrementAndGet();

      if (failureMode == FailureMode.FAIL_TWICE_THEN_SUCCEED && currentAttempt < 3) {
        throw new RetryableEventProcessingException("Temporary dependency failure");
      }

      if (failureMode == FailureMode.FAIL_ALWAYS_NON_RETRYABLE) {
        throw new NonRetryableEventProcessingException("Poison payload");
      }

      sideEffects.incrementAndGet();
      assertThat(event.payload().paymentId()).isEqualTo("pay-1");
    }

    int attempts() {
      return attempts.get();
    }

    int sideEffects() {
      return sideEffects.get();
    }

    void reset() {
      attempts.set(0);
      sideEffects.set(0);
    }

    enum FailureMode {
      FAIL_TWICE_THEN_SUCCEED,
      FAIL_ALWAYS_NON_RETRYABLE
    }
  }
}
