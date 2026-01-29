package com.example.trading.api;

import com.example.trading.api.dto.PlaceOrderRequest;
import com.example.trading.api.dto.PlaceOrderResponse;
import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.entity.TradeOrder;
import com.example.trading.persistence.repo.TradeOrderRepository;
import com.example.trading.service.CurrentUserService;
import com.example.trading.service.OrderService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrdersApiController {

  private final CurrentUserService currentUser;
  private final OrderService orders;
  private final TradeOrderRepository orderRepo;

  public OrdersApiController(CurrentUserService currentUser, OrderService orders, TradeOrderRepository orderRepo) {
    this.currentUser = currentUser;
    this.orders = orders;
    this.orderRepo = orderRepo;
  }

  @PostMapping
  public ResponseEntity<PlaceOrderResponse> place(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @RequestBody PlaceOrderRequest req,
      Authentication auth
  ) {
    return ResponseEntity.ok(orders.placeOrder(idempotencyKey, req, auth));
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable UUID id, Authentication auth) {
    AppUser user = currentUser.requireUser(auth);
    TradeOrder o = orderRepo.findByIdAndUser(id, user).orElseThrow();
    return Map.of(
        "id", o.getId(),
        "status", o.getStatus(),
        "symbol", o.getSymbol(),
        "side", o.getSide(),
        "orderType", o.getOrderType(),
        "quantity", o.getQuantity(),
        "limitPrice", o.getLimitPrice(),
        "createdAt", o.getCreatedAt(),
        "updatedAt", o.getUpdatedAt(),
        "schwabOrderId", o.getSchwabOrderId()
    );
  }

  @GetMapping
  public List<?> list(@RequestParam(required = false) String status, Authentication auth) {
    AppUser user = currentUser.requireUser(auth);
    if (status == null || status.isBlank()) {
      return orderRepo.findTop200ByUserOrderByCreatedAtDesc(user);
    }
    return orderRepo.findTop200ByUserAndStatusOrderByCreatedAtDesc(user, TradeOrder.Status.valueOf(status));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<?> cancel(@PathVariable UUID id, Authentication auth) {
    orders.cancelOrder(id, auth);
    return ResponseEntity.ok(Map.of("canceled", true));
  }
}
