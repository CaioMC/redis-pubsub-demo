package io.github.caiomc.pubsub.adapters.room.web.response;

import java.time.Instant;

public record MessageResponse(
        String room,
        String author,
        String text,
        String origin,
        Instant sentAt
) {
}
