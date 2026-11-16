package io.github.caiomc.pubsub.adapters.room.web.response;

public record StreamReadyResponse(
        String room,
        String instance
) {
}
