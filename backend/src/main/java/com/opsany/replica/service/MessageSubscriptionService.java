package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.opsany.replica.domain.MessageSubscription;
import com.opsany.replica.dto.MessageSubscriptionPayload;
import com.opsany.replica.repository.MessageSubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageSubscriptionService {

    private final MessageSubscriptionRepository messageSubscriptionRepository;

    public List<MessageSubscription> listByUsername(String username) {
        return messageSubscriptionRepository.findByUsername(username);
    }

    public void saveSubscriptions(String username, List<MessageSubscriptionPayload> payloads) {
        LocalDateTime now = LocalDateTime.now();
        for (MessageSubscriptionPayload payload : payloads) {
            MessageSubscription existing = messageSubscriptionRepository.findOne(
                username,
                payload.getMessageType(),
                payload.getSource()
            );
            if (existing == null) {
                messageSubscriptionRepository.insert(MessageSubscription.builder()
                    .username(username)
                    .messageType(payload.getMessageType())
                    .source(payload.getSource())
                    .siteEnabled(Boolean.TRUE.equals(payload.getSiteEnabled()))
                    .smsEnabled(Boolean.TRUE.equals(payload.getSmsEnabled()))
                    .mailEnabled(Boolean.TRUE.equals(payload.getMailEnabled()))
                    .wxEnabled(Boolean.TRUE.equals(payload.getWxEnabled()))
                    .dingEnabled(Boolean.TRUE.equals(payload.getDingEnabled()))
                    .updatedAt(now)
                    .build());
                continue;
            }

            existing.setSiteEnabled(Boolean.TRUE.equals(payload.getSiteEnabled()));
            existing.setSmsEnabled(Boolean.TRUE.equals(payload.getSmsEnabled()));
            existing.setMailEnabled(Boolean.TRUE.equals(payload.getMailEnabled()));
            existing.setWxEnabled(Boolean.TRUE.equals(payload.getWxEnabled()));
            existing.setDingEnabled(Boolean.TRUE.equals(payload.getDingEnabled()));
            existing.setUpdatedAt(now);
            messageSubscriptionRepository.update(existing);
        }
    }
}
