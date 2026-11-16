package io.github.caiomc.pubsub.adapters.room.web.mapper;

import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import io.github.caiomc.pubsub.adapters.room.web.request.SendMessageRequest;
import io.github.caiomc.pubsub.adapters.room.web.response.MessageResponse;
import io.github.caiomc.pubsub.core.room.domain.model.Room;
import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;
import io.github.caiomc.pubsub.core.room.usecase.command.PublishMessageCommand;
import io.github.caiomc.pubsub.core.room.usecase.command.SubscribeRoomCommand;

import static java.util.Objects.isNull;

@Component
public class RoomWebMapper {

    public PublishMessageCommand toCommand(
            String room,
            SendMessageRequest request
    ) {
        if (isNull(request)) {
            throw new IllegalArgumentException("Corpo da requisição obrigatório: {\"author\": \"...\", \"text\": \"...\"}");
        }

        return new PublishMessageCommand(new Room(room), request.author(), request.text());
    }

    public SubscribeRoomCommand toSubscribeCommand(
            String room,
            Consumer<RoomMessage> listener
    ) {
        return new SubscribeRoomCommand(new Room(room), listener);
    }

    public MessageResponse toResponse(RoomMessage message) {
        return new MessageResponse(
                message.room().name(),
                message.author(),
                message.text(),
                message.origin(),
                message.sentAt()
        );
    }
}
