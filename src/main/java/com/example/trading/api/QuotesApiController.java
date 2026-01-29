package com.example.trading.api;

import com.example.trading.service.SchwabTokenService;
import com.example.trading.schwab.SchwabClient;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quotes")
public class QuotesApiController {

  private final SchwabTokenService tokens;
  private final SchwabClient schwab;

  public QuotesApiController(SchwabTokenService tokens, SchwabClient schwab) {
    this.tokens = tokens;
    this.schwab = schwab;
  }

  @GetMapping
  public Map<String, Object> quotes(@RequestParam String symbols, Authentication auth) {
    String token = tokens.requireAccessToken(auth);
    List<String> list = Arrays.stream(symbols.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();

    var dto = schwab.getQuotes(token, list);
    return Map.of("quotes", dto.quotesBySymbol());
  }
}
