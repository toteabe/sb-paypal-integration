package org.iesvdm.payment.paypal.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iesvdm.payment.paypal.config.PaypalConfig;
import org.iesvdm.payment.paypal.model.PaypalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaypalService {

    private final RestTemplate restTemplate;
    private final PaypalConfig paypalConfig;

    private String accessToken;
    private LocalDateTime accessTokenExpiry = Instant.ofEpochMilli(0).atZone(ZoneId.systemDefault()).toLocalDateTime();
    private ObjectMapper objectMapper = new ObjectMapper();

    public String generateAccessToken() throws JsonProcessingException {

        String uri = paypalConfig.getBaseUrl()+ "/v1/oauth2/token";

        HttpHeaders httpHeaders = new HttpHeaders() {
            {
                String auth = paypalConfig.getClientId() + ":" + paypalConfig.getClientSecret();
                log.info(auth);
                String encodedAuth =  java.util.Base64.getEncoder()
                        .withoutPadding() //<<< OJO!
                        .encodeToString(auth.getBytes(StandardCharsets.UTF_8));//<<< OJO!

                String authHeader = "Basic " + encodedAuth;
                log.info("authHeader {}", authHeader );
                set( "Authorization", authHeader );
                set( "Content-Type", "application/x-www-form-urlencoded");
            }
        };

        LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("grant_type","client_credentials");
        params.add("ignoreCache", true);
        params.add("return_authn_schemes", true);
        params.add("return_client_metadata", true);
        params.add("return_unconsented_scopes", true);

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.POST,
                new HttpEntity<>(params, httpHeaders), String.class);

        JsonNode jsonNode = objectMapper.readTree(res.getBody());

        this.accessToken = jsonNode.path("access_token").asText();
        long expiresIn = jsonNode.path("expires_in").asLong();
        this.accessTokenExpiry = LocalDateTime.now().plusSeconds(expiresIn);
        String clientId = jsonNode.path("access_token_for").asText();

        log.info("accesToken = {}",  accessToken);
        log.info("expires_in = {}", expiresIn);
        log.info("access_token_for = {}", clientId);

        if (!jsonNode.path("client_metadata").isMissingNode()
                && jsonNode.path("client_metadata").path("display_name").isValueNode()) {
            log.info("Login en paypal mediante: {}", jsonNode.path("client_metadata")
                    .path("display_name").asText()) ;
        }

        return this.accessToken;

    }

    public PaypalResponse createOrder(String intent,
                                      String currencyCode,
                                      String value,
                                      String returnUrl,
                                      String cancelUrl
                                ) throws JsonProcessingException {

        if (LocalDateTime.now().isAfter(this.accessTokenExpiry.minusMinutes(20))) {
            this.generateAccessToken();
        }

        String uri = paypalConfig.getBaseUrl()+ "/v2/checkout/orders";

        HttpHeaders httpHeaders = new HttpHeaders() {
            {
                String authHeader = "Bearer " + accessToken;
                log.info("authHeader {}", authHeader);
                set( "Authorization", authHeader );
                set( "Content-Type", "application/json");
            }
        };

        String orderJson = String.format("""
				{
				    "intent": "%s",
				    "purchase_units": [
				        {
				            "amount": {
				                "currency_code": "%s",
				                "value": "%s"
				            }
				        }
				    ],
				    "payment_source": {
				        "paypal": {
				            "experience_context": {
				                "return_url": "%s",
				                "cancel_url": "%s"
				            }
				        }
				    }
				}""", intent, currencyCode, value, returnUrl, cancelUrl);

        log.info("order\n{}", orderJson);

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.POST,
                new HttpEntity<>(orderJson, httpHeaders), String.class);

        JsonNode jsonNode = objectMapper.readTree(res.getBody());

        if (jsonNode.path("id").isMissingNode()) {
            throw new IllegalArgumentException("Falta id en respuesta Paypal:\n" + jsonNode.toString());
        }

        String orderId  = jsonNode.path("id").asText();
        log.info("orderId = {}", orderId);

        JsonNode arrayLinks = jsonNode.path("links");

        String href = null;
        if (arrayLinks.isArray()) {

            for (final JsonNode linkNode : arrayLinks) {

                if (linkNode.path("rel").asText().equals("payer-action")) {
                    href = linkNode.path("href").asText();
                    break;
                }

            }

        }

        log.info("href {}", href);

        return new PaypalResponse(href, orderId);
    }

    public ResponseEntity<String> showOrderDetails(String orderId) throws JsonProcessingException {
        String uri = paypalConfig.getBaseUrl()+ "/v2/checkout/orders/" + orderId;

        HttpHeaders httpHeaders = new HttpHeaders() {
            {
                String authHeader = "Bearer " + accessToken;
                log.info("authHeader {}", authHeader);
                set( "Authorization", authHeader );
            }
        };

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET,
                                                                    new HttpEntity<>(httpHeaders), String.class);

        JsonNode jsonNode = objectMapper.readTree(res.getBody());

        String status = jsonNode.path("status").asText();
        log.info("status = {}",status);

        return new ResponseEntity<String>(String.format("""
                                                    {"state": "%s"}""", status), HttpStatus.OK);
    }

}
