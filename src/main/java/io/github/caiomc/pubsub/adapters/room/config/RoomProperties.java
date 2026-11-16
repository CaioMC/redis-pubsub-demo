package io.github.caiomc.pubsub.adapters.room.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("demo.room")
public record RoomProperties(String instanceId) {
}
