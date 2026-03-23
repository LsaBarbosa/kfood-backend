package com.kfood.payment.app;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentWebhookSecurityProperties.class)
public class PaymentWebhookSecurityConfig {}
