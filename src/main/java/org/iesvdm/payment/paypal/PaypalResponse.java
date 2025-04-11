package org.iesvdm.payment.paypal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaypalResponse {

    private boolean ok;
    private String approvalUrl;


}
