package com.example.trading.poller;

import com.example.trading.persistence.entity.TradeOrder;
import com.example.trading.persistence.repo.ExecutionFillRepository;
import com.example.trading.persistence.repo.TradeOrderRepository;
import com.example.trading.service.SchwabTokenService;
import com.example.trading.schwab.SchwabClient;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Poll open orders every 5s and reconcile status/fills into DB.
 * Single-instance assumption (approved).
 */
@Component
public class OrderReconciliationJob {
  private static final Logger log = LoggerFactory.getLogger(OrderReconciliationJob.class);

  private final TradeOrderRepository orders;
  private final ExecutionFillRepository fills;
  private final SchwabClient schwab;
  private final SchwabTokenService tokens;

  private final long intervalMs;

  public OrderReconciliationJob(
      TradeOrderRepository orders,
      ExecutionFillRepository fills,
      SchwabClient schwab,
      SchwabTokenService tokens,
      @Value("${app.polling.orders-ms:5000}") long intervalMs
  ) {
    this.orders = orders;
    this.fills = fills;
    this.schwab = schwab;
    this.tokens = tokens;
    this.intervalMs = intervalMs;
  }

  @Scheduled(fixedDelayString = "${app.polling.orders-ms:5000}")
  @Transactional
  public void run() {
    List<TradeOrder> reconcilable = orders.findReconcilable(List.of(
        TradeOrder.Status.SUBMITTED,
        TradeOrder.Status.OPEN,
        TradeOrder.Status.PARTIALLY_FILLED,
        TradeOrder.Status.CANCEL_REQUESTED
    ));

    if (reconcilable.isEmpty()) return;

    // TODO: implement per-user batching, token acquisition per user, backoff on 429
    // This skeleton intentionally leaves Schwab calls unimplemented.
    log.debug("Reconciling {} orders at {}", reconcilable.size(), Instant.now());
  }
}
