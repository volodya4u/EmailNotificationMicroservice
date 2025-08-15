package com.appsdeveloperblog.ws.emailnotification.handler;

import com.appsdeveloperblog.ws.core.ProductCreatedEvent;
import com.appsdeveloperblog.ws.emailnotification.error.NotRetryableException;
import com.appsdeveloperblog.ws.emailnotification.error.RetryableException;
import com.appsdeveloperblog.ws.emailnotification.io.ProcessedEventEntity;
import com.appsdeveloperblog.ws.emailnotification.io.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@KafkaListener(topics="product-created-events-topic")
public class ProductCreatedEventHandler {

    private final RestTemplate restTemplate;

    private ProcessedEventRepository processedEventRepository;

    public ProductCreatedEventHandler(RestTemplate restTemplate,
                                      ProcessedEventRepository processedEventRepository) {
        this.restTemplate = restTemplate;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaHandler
    public void handle(@Payload ProductCreatedEvent productCreatedEvent,
                       @Header("messageId") String messageId,
                       @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
        log.info("Received a new event {} with productId: {}",
                productCreatedEvent.getTitle(),  productCreatedEvent.getProductId());

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

        // Save a unique message id in a database table
        try {
            processedEventRepository.save(new ProcessedEventEntity(messageId, productCreatedEvent.getProductId()));
        } catch (DataIntegrityViolationException ex) {
            throw new NotRetryableException(ex);
        }
    }
}
