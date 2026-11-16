package io.github.caiomc.pubsub.core.room.usecase.command;

import java.util.function.Consumer;

import io.github.caiomc.pubsub.core.room.domain.model.Room;
import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;

import static java.util.Objects.isNull;

public record SubscribeRoomCommand(
        Room room,
        Consumer<RoomMessage> listener
) {

    public SubscribeRoomCommand {
        if (isNull(room) || isNull(listener)) {
            throw new IllegalArgumentException("A assinatura precisa de sala e de ouvinte");
        }
    }
}
