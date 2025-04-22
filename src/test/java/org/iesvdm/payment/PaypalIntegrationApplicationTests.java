package org.iesvdm.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.assertj.core.api.Assertions;
import org.iesvdm.payment.paypal.config.PaypalConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
class PaypalIntegrationApplicationTests {

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	PaypalConfig paypalConfig;

	private static String accessToken;
	private static String href;
	private static String orderId;
	private static ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@Order(1)
	void accessToken() throws JsonProcessingException {

		//https://developer.paypal.com/docs/api/orders/v2/#orders_create
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

		accessToken = jsonNode.path("access_token").asText();
		long expiresIn = jsonNode.path("expires_in").asLong();
        LocalDateTime accessTokenExpiry = LocalDateTime.now().plusSeconds(expiresIn);
		String clientId = jsonNode.path("access_token_for").asText();

		log.info("accesToken = {}",  accessToken);
		log.info("expires_in = {}", expiresIn);
		log.info("access_token_for = {}", clientId);

		if (!jsonNode.path("client_metadata").isMissingNode()
				&& jsonNode.path("client_metadata").path("display_name").isValueNode()) {
			log.info("Login en paypal mediante: {}", jsonNode.path("client_metadata")
																.path("display_name").asText()) ;
		}

		Assertions.assertThat(accessToken).isNotBlank();

    }

	/**POST https://api-m.sandbox.paypal.com/v2/checkout/orders
	 * {
	 *     "intent": "CAPTURE",
	 *     "purchase_units": [
	 *         {
	 *             "amount": {
	 *                 "currency_code": "USD",
	 *                 "value": "101.00"
	 *             }
	 *         }
	 *     ],
	 *     "payment_source": {
	 *         "paypal": {
	 *             "experience_context": {
	 *                 "return_url": "https://example.com/returnUrl",
	 *                 "cancel_url": "https://example.com/cancelUrl"
	 *             }
	 *         }
	 *     }
	 * }
	 * @throws JsonProcessingException
	 */
	@Test
	@Order(2)
	void checkoutOrders() throws JsonProcessingException {

		String uri = paypalConfig.getBaseUrl()+ "/v2/checkout/orders";

		HttpHeaders httpHeaders = new HttpHeaders() {
			{
				String authHeader = "Bearer " + accessToken;
				log.info("authHeader {}", authHeader);
				set( "Authorization", authHeader );
				set( "Content-Type", "application/json");
			}
		};

//		JSONObject order = new JSONObject();
//		try {
//			order.put("intent", "CAPTURE");
//
//			JSONObject purchaseUnit = new JSONObject();
//
//			JSONObject amount = new JSONObject();
//			amount.put("currency_code", "EUR");
//			amount.put("value", "100.00");
//
//			purchaseUnit.put("amount", amount);
//
//			JSONArray arrayPurchaseUnits = new JSONArray();
//			arrayPurchaseUnits.put(purchaseUnit);
//
//			order.put("purchase_units", arrayPurchaseUnits);
//
//		} catch (JSONException e) {
//			log.error(e.getMessage());
//			Assertions.fail();
//		}
//		String orderJson = order.toString();

		String intent = "CAPTURE"; //Ver tb AUTHORIZE
		String currencyCode = "EUR";
		String value = "100.00";
		String returnUrl = "https://example.com/returnUrl";
		String cancelUrl = "https://example.com/cancelUrl";

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
			Assertions.fail();
		}

		this.orderId  = jsonNode.path("id").asText();
		log.info("orderId = {}", orderId);

		JsonNode arrayLinks = jsonNode.path("links");

		href = null;
		if (arrayLinks.isArray()) {

			for (final JsonNode linkNode : arrayLinks) {

				if (linkNode.path("rel").asText().equals("payer-action")) {
					href = linkNode.path("href").asText();
					break;
				}

			}

		}

		log.info("href {}", href);

		Assertions.assertThat(href).isNotNull();

	}


	@Order(3)
	@Test
	void showOrderDetails() throws JsonProcessingException {

		String uri = paypalConfig.getBaseUrl()+ "/v2/checkout/orders/{id}";

		uri = uri.replace("{id}", this.orderId);

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

		Assertions.assertThat(status).isEqualTo("PAYER_ACTION_REQUIRED");

	}
}
