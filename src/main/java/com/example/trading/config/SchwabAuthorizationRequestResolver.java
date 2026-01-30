package com.example.trading.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public class SchwabAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
  private static final String SCHWAB_REGISTRATION_ID = "schwab";
  private static final String CLIENT_SECRET = "client_secret";
  private static final String NONCE = "nonce";
  private static final String CODE_CHALLENGE = "code_challenge";
  private static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
  private static final String CODE_VERIFIER = "code_verifier";

  private final ClientRegistrationRepository clientRegistrationRepository;
  private final DefaultOAuth2AuthorizationRequestResolver delegate;
  private final String authorizationRequestBaseUri;

  public SchwabAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
    this(
        clientRegistrationRepository,
        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
    );
  }

  SchwabAuthorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository,
      String authorizationRequestBaseUri
  ) {
    this.clientRegistrationRepository = clientRegistrationRepository;
    this.authorizationRequestBaseUri = authorizationRequestBaseUri;
    this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository,
        authorizationRequestBaseUri
    );
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    String registrationId = resolveRegistrationId(request);
    if (registrationId == null) {
      return null;
    }
    return customize(delegate.resolve(request, registrationId), registrationId);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
    return customize(delegate.resolve(request, registrationId), registrationId);
  }

  private OAuth2AuthorizationRequest customize(
      OAuth2AuthorizationRequest authorizationRequest,
      String registrationId
  ) {
    if (authorizationRequest == null || !SCHWAB_REGISTRATION_ID.equals(registrationId)) {
      return authorizationRequest;
    }

    ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
    Map<String, Object> additionalParameters = new LinkedHashMap<>(
        authorizationRequest.getAdditionalParameters()
    );
    additionalParameters.remove(NONCE);
    additionalParameters.remove(CODE_CHALLENGE);
    additionalParameters.remove(CODE_CHALLENGE_METHOD);

    if (registration != null && registration.getClientSecret() != null
        && !registration.getClientSecret().isBlank()) {
      additionalParameters.put(CLIENT_SECRET, registration.getClientSecret());
    }

    Map<String, Object> attributes = new LinkedHashMap<>(authorizationRequest.getAttributes());
    attributes.remove(CODE_VERIFIER);
    attributes.remove(NONCE);

    return OAuth2AuthorizationRequest.from(authorizationRequest)
        .additionalParameters(params -> {
          params.clear();
          params.putAll(additionalParameters);
        })
        .attributes(attrs -> {
          attrs.clear();
          attrs.putAll(attributes);
        })
        .build();
  }

  private String resolveRegistrationId(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (!requestUri.startsWith(contextPath)) {
      return null;
    }
    String path = requestUri.substring(contextPath.length());
    String prefix = authorizationRequestBaseUri + "/";
    if (!path.startsWith(prefix)) {
      return null;
    }
    String registrationId = path.substring(prefix.length());
    return registrationId.isEmpty() ? null : registrationId;
  }
}
