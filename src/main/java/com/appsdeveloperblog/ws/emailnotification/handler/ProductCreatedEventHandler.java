package com.appsdeveloperblog.ws.emailnotification.handler;

import com.appsdeveloperblog.ws.core.ProductCreatedEvent;
import com.appsdeveloperblog.ws.emailnotification.error.NotRetryableException;
import com.appsdeveloperblog.ws.emailnotification.error.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@KafkaListener(topics="product-created-events-topic")
public class ProductCreatedEventHandler {

    private final RestTemplate restTemplate;

    public ProductCreatedEventHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @KafkaHandler
    public void handle(ProductCreatedEvent productCreatedEvent) {
        log.info("Received a new event {}", productCreatedEvent.getTitle());

        String requestUrl = "http://localhost:8082";

        try {
            ResponseEntity<String> response
                    = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Received response from a remote service: {}", response.getBody());
            }
        } catch (ResourceAccessException e) {
            log.error(e.getMessage());
            throw new RetryableException(e);
        } catch (HttpServerErrorException e) {
            log.error(e.getMessage());
            throw new NotRetryableException(e);
        }
    }
}
