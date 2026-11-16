package io.github.caiomc.pubsub.adapters.room.web.mapper;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.caiomc.pubsub.adapters.room.web.request.SendMessageRequest;
import io.github.caiomc.pubsub.adapters.room.web.response.MessageResponse;
import io.github.caiomc.pubsub.core.room.domain.model.Room;
import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;
import io.github.caiomc.pubsub.core.room.usecase.command.PublishMessageCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomWebMapperTest {

    private final RoomWebMapper mapper = new RoomWebMapper();

    @Test
    void mapsRequestToCommand() {
        PublishMessageCommand command = this.mapper.toCommand("geral", new SendMessageRequest("ana", "oi"));

        assertThat(command).isEqualTo(new PublishMessageCommand(new Room("geral"), "ana", "oi"));
    }

    @Test
    void rejectsMissingBody() {
        assertThatThrownBy(() -> this.mapper.toCommand("geral", null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Corpo");
    }

    @Test
    void mapsMessageToResponse() {
        Instant now = Instant.parse("2026-11-16T12:00:00Z");
        RoomMessage message = new RoomMessage(new Room("geral"), "ana", "oi", "instancia-a", now);

        MessageResponse response = this.mapper.toResponse(message);

        assertThat(response).isEqualTo(new MessageResponse("geral", "ana", "oi", "instancia-a", now));
    }
}
