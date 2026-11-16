package io.github.caiomc.pubsub.core.room.usecase;

import io.github.caiomc.pubsub.core.room.usecase.command.SubscribeRoomCommand;

public interface SubscribeRoomUseCase {

    RoomSubscription subscribe(SubscribeRoomCommand command);
}
