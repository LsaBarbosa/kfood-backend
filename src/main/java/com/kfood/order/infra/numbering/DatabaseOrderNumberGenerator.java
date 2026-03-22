package com.kfood.order.infra.numbering;

import com.kfood.order.app.OrderNumberGenerator;
import com.kfood.order.infra.persistence.SalesOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DatabaseOrderNumberGenerator implements OrderNumberGenerator {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final String PREFIX = "PED";
  private static final int SEQUENCE_WIDTH = 6;

  @PersistenceContext private EntityManager entityManager;

  private final Clock clock;

  public DatabaseOrderNumberGenerator() {
    this(Clock.systemUTC());
  }

  DatabaseOrderNumberGenerator(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock is required");
  }

  @Override
  public String next(SalesOrder order) {
    var sequenceValue =
        ((Number)
                entityManager
                    .createNativeQuery("select nextval('sales_order_number_seq')")
                    .getSingleResult())
            .longValue();
    var zoneId = resolveZone(order);
    var datePart = ZonedDateTime.now(clock).withZoneSameInstant(zoneId).format(DATE_FORMATTER);
    var sequencePart = String.format("%0" + SEQUENCE_WIDTH + "d", sequenceValue);
    return PREFIX + "-" + datePart + "-" + sequencePart;
  }

  private ZoneId resolveZone(SalesOrder order) {
    var timezone = order.getStore().getTimezone();
    if (timezone == null || timezone.isBlank()) {
      return ZoneId.of("America/Sao_Paulo");
    }
    return ZoneId.of(timezone);
  }
}
