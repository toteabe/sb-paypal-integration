package org.iesvdm.payment.paypal.config;


import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "paypal")
public class PaypalConfig {


    private String baseUrl;
    private String clientId;
    private String clientSecret;

    @Bean
    public RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

}