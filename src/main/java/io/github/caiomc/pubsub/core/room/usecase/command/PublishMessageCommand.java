package io.github.caiomc.pubsub.core.room.usecase.command;

import io.github.caiomc.pubsub.core.room.domain.model.Room;

public record PublishMessageCommand(
        Room room,
        String author,
        String text
) {
}
