package com.ra.medicine.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ra.medicine.model.dto.PharmacyAlertMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacyAlertSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;

    @Getter
    private volatile PharmacyAlertMessage lastReceivedAlert;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            PharmacyAlertMessage alert = objectMapper.readValue(body, PharmacyAlertMessage.class);
            this.lastReceivedAlert = alert;

            log.info("================================================================================");
            log.info("[REDIS SUBSCRIBER] Received message from channel '{}':", channel);
            log.info("Type: {}", alert.type());
            log.info("Message: {}", alert.message());
            log.info("Raw JSON: {}", body);
            log.info("================================================================================");
        } catch (Exception e) {
            log.error("[REDIS SUBSCRIBER] Failed to process message from channel: {}", e.getMessage(), e);
        }
    }
}
