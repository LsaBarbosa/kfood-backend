package com.kfood;

import com.kfood.shared.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ConfigurationPropertiesScan(basePackageClasses = AppProperties.class)
public class KfoodBackendApplication {

  static void main(String[] args) {
    SpringApplication.run(KfoodBackendApplication.class, args);
  }
}
