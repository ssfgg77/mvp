package com.example.trading.web;

import com.example.trading.persistence.entity.AppUser;
import com.example.trading.persistence.entity.TradeOrder;
import com.example.trading.persistence.repo.ExecutionFillRepository;
import com.example.trading.persistence.repo.TradeOrderRepository;
import com.example.trading.service.CurrentUserService;
import com.example.trading.service.OrderService;
import com.example.trading.service.UserSettingsService;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class OrdersController {

  private final CurrentUserService currentUser;
  private final TradeOrderRepository orders;
  private final ExecutionFillRepository fills;
  private final OrderService orderService;
  private final UserSettingsService settings;

  public OrdersController(CurrentUserService currentUser, TradeOrderRepository orders, ExecutionFillRepository fills, OrderService orderService, UserSettingsService settings) {
    this.currentUser = currentUser;
    this.orders = orders;
    this.fills = fills;
    this.orderService = orderService;
    this.settings = settings;
  }

  @GetMapping("/orders")
  public String ordersPage(@RequestParam(required = false) String status, Model model) {
    model.addAttribute("status", status == null ? "OPEN" : status);
    return "orders";
  }

  @GetMapping("/orders/fragment")
  public String ordersFragment(@RequestParam(required = false) String status, Authentication auth, Model model) {
    AppUser user = currentUser.requireUser(auth);
    String s = (status == null || status.isBlank()) ? "OPEN" : status;

    var list = orders.findTop200ByUserAndStatusOrderByCreatedAtDesc(user, TradeOrder.Status.valueOf(s));
    model.addAttribute("orders", list);
    model.addAttribute("status", s);
    return "orders-fragment :: table";
  }

  @GetMapping("/orders/new")
  public String newOrderForm(Authentication auth) {
    AppUser user = currentUser.requireUser(auth);
    var s = settings.getOrCreate(user);
    if (s.getDefaultSchwabAccount() == null) {
      return "redirect:/settings?chooseDefault=1";
    }
    return "orders-new";
  }

  @GetMapping("/orders/{id}")
  public String orderDetail(@PathVariable UUID id, Authentication auth, Model model) {
    AppUser user = currentUser.requireUser(auth);
    TradeOrder o = orders.findByIdAndUser(id, user).orElseThrow();
    model.addAttribute("order", o);
    model.addAttribute("fills", fills.findAllByOrderOrderByExecTimeAsc(o));
    return "order-detail";
  }

  @PostMapping("/orders/{id}/cancel")
  public String cancel(@PathVariable UUID id, Authentication auth) {
    orderService.cancelOrder(id, auth);
    return "redirect:/orders/" + id + "?canceled";
  }
}
