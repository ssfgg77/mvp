package com.example.trading.schwab.impl;

import com.example.trading.schwab.SchwabClient;
import com.example.trading.schwab.dto.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SchwabClientImpl implements SchwabClient {

  private final RestClient rest;
  private final String apiBaseUrl;

  public SchwabClientImpl(RestClient rest, @Value("${app.schwab.api-base-url}") String apiBaseUrl) {
    this.rest = rest;
    this.apiBaseUrl = apiBaseUrl;
  }

  @Override
  public List<SchwabAccountDto> getAccounts(String accessToken) {
    // TODO: Implement per Schwab Accounts endpoint
    throw new UnsupportedOperationException("TODO: implement Schwab getAccounts()");
  }

  @Override
  public AccountSnapshotDto getDefaultAccountSnapshot(String accessToken, String schwabAccountRef) {
    // TODO: Implement balances+positions fetch for a single account
    throw new UnsupportedOperationException("TODO: implement Schwab getDefaultAccountSnapshot()");
  }

  @Override
  public QuotesDto getQuotes(String accessToken, List<String> symbols) {
    // TODO: Implement quotes endpoint
    throw new UnsupportedOperationException("TODO: implement Schwab getQuotes()");
  }

  @Override
  public PlaceOrderResultDto placeOrder(String accessToken, String schwabAccountRef, PlaceOrderRequestDto request) {
    // TODO: Implement place order endpoint
    throw new UnsupportedOperationException("TODO: implement Schwab placeOrder()");
  }

  @Override
  public CancelOrderResultDto cancelOrder(String accessToken, String schwabAccountRef, String schwabOrderId) {
    // TODO: Implement cancel endpoint
    throw new UnsupportedOperationException("TODO: implement Schwab cancelOrder()");
  }

  @Override
  public OrderStatusDto getOrderStatus(String accessToken, String schwabAccountRef, String schwabOrderId) {
    // TODO: Implement order status endpoint
    throw new UnsupportedOperationException("TODO: implement Schwab getOrderStatus()");
  }
}
