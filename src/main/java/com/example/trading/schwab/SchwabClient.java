package com.example.trading.schwab;

import com.example.trading.schwab.dto.*;
import java.util.List;

/**
 * Schwab API boundary. Implement these with RestClient/WebClient per Schwab docs.
 * All methods must be authenticated with a valid Bearer token.
 */
public interface SchwabClient {

  List<SchwabAccountDto> getAccounts(String accessToken);

  AccountSnapshotDto getDefaultAccountSnapshot(String accessToken, String schwabAccountRef);

  QuotesDto getQuotes(String accessToken, List<String> symbols);

  PlaceOrderResultDto placeOrder(String accessToken, String schwabAccountRef, PlaceOrderRequestDto request);

  CancelOrderResultDto cancelOrder(String accessToken, String schwabAccountRef, String schwabOrderId);

  OrderStatusDto getOrderStatus(String accessToken, String schwabAccountRef, String schwabOrderId);
}
