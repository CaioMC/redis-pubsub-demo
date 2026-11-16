package io.github.caiomc.pubsub.adapters.room.web.request;

public record SendMessageRequest(
        String author,
        String text
) {
}
