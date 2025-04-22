package org.iesvdm.payment.paypal.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.iesvdm.payment.paypal.model.PaypalRequest;
import org.iesvdm.payment.paypal.model.PaypalResponse;
import org.iesvdm.payment.paypal.service.PaypalService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin("http://localhost:4200")
@Slf4j
public class PaypalController {

    private final PaypalService paypalService;

    @PostMapping("/payment/create")
    public PaypalResponse createPayment(@RequestBody PaypalRequest paypalRequest
    ) throws JsonProcessingException {

            String cancelUrl = "http://localhost:4200/payment/cancel";
            String successUrl = "http://localhost:4200/payment/success";

            return paypalService.createOrder("CAPTURE",
                    paypalRequest.getCurrency(),
                    paypalRequest.getAmount().toPlainString(),
                    successUrl, cancelUrl);

    }

    @GetMapping("/payment/success")
    public ResponseEntity<String> paymentSuccess(
            @RequestParam("orderId") String orderId
    ) throws JsonProcessingException {

        return this.paypalService.showOrderDetails(orderId);

    }


}
