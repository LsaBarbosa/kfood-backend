package com.kfood;

import com.kfood.shared.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = AppProperties.class)
public class KfoodBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(KfoodBackendApplication.class, args);
  }
}
