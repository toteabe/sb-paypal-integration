package org.iesvdm.payment.paypal.model;


import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public class PaypalRequest {

    private final String method;
    private final BigDecimal amount;
    private final String currency;
    private final String description;

}
