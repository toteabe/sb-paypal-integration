package org.iesvdm.payment.paypal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaypalResponse {

    private String href;
    private String orderId;

}
