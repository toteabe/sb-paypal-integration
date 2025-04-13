package org.iesvdm.payment.paypal;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequiredArgsConstructor
@CrossOrigin("http://localhost:4200")
@Slf4j
public class PaypalController {

    private final PaypalService paypalService;

    @PostMapping("/payment/create")
    public PaypalResponse createPayment(@RequestBody PaypalRequest paypalRequest
    ) {
        try {
            String cancelUrl = "http://localhost:4200/payment/cancel";
            String successUrl = "http://localhost:4200/payment/success";
            Payment payment = paypalService.createPayment(
                    paypalRequest.getAmount(),
                    paypalRequest.getCurrency(),
                    paypalRequest.getMethod(),
                    "sale",
                    paypalRequest.getDescription(),
                    cancelUrl,
                    successUrl
            );

            for (Links links: payment.getLinks()) {
                if (links.getRel().equals("approval_url")) {
                    return new PaypalResponse(true,links.getHref());
                }
            }
        } catch (PayPalRESTException e) {
            log.error("Error occurred:: ", e);
        }
        return new PaypalResponse(false, null);
    }

    @GetMapping("/payment/success")
    public ResponseEntity<String> paymentSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId
    ) {
        try {
            Payment payment = paypalService.executePayment(paymentId, payerId);

            return new ResponseEntity<>("{\"state\": \""+payment.getState()+"\"}", HttpStatus.OK);

        } catch (PayPalRESTException e) {
            log.error("Error occurred:: ", e);
            if (e.getMessage().contains("PAYMENT_ALREADY_DONE")) { return new ResponseEntity<>("{\"state\": \"approved\"}", HttpStatus.OK); }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
