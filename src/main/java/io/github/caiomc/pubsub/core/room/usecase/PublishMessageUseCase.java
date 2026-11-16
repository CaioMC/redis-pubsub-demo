package io.github.caiomc.pubsub.core.room.usecase;

import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;
import io.github.caiomc.pubsub.core.room.usecase.command.PublishMessageCommand;

public interface PublishMessageUseCase {

    RoomMessage publish(PublishMessageCommand command);
}
