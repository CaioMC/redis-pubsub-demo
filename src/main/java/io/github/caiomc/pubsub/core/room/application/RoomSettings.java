package io.github.caiomc.pubsub.core.room.application;

import static java.util.Objects.isNull;

/**
 * @param instanceId nome desta instância; vai como {@code origin} nas mensagens publicadas por ela
 */
public record RoomSettings(String instanceId) {

    public RoomSettings {
        if (isNull(instanceId) || instanceId.isBlank()) {
            throw new IllegalArgumentException("Configure demo.room.instance-id");
        }
    }
}
