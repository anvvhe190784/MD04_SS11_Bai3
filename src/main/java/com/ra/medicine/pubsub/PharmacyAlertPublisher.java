package com.ra.medicine.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ra.medicine.model.dto.PharmacyAlertMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacyAlertPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ChannelTopic pharmacyAlertsTopic;
    private final ObjectMapper objectMapper;

    public void publishAlert(PharmacyAlertMessage alertMessage) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(alertMessage);
            stringRedisTemplate.convertAndSend(pharmacyAlertsTopic.getTopic(), jsonMessage);
            log.info("[REDIS PUBLISHER] Published alert to channel '{}': {}",
                    pharmacyAlertsTopic.getTopic(), jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("[REDIS PUBLISHER] Failed to serialize alert message: {}", e.getMessage(), e);
            throw new RuntimeException("Error serializing alert message", e);
        }
    }
}
