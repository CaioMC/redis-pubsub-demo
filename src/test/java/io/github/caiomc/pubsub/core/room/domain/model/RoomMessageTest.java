package io.github.caiomc.pubsub.core.room.domain.model;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomMessageTest {

    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    @Test
    void createsValidMessage() {
        RoomMessage message = this.message("ana", "oi");

        assertThat(message.room().name()).isEqualTo("geral");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Geral", "sala com espaço", "chat:*", "a234567890123456789012345678901234567890x"})
    void rejectsUnsafeRoomName(String name) {
        assertThatThrownBy(() -> new Room(name)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("a-z, 0-9");
    }

    @Test
    void rejectsBlankAuthor() {
        assertThatThrownBy(() -> this.message(" ", "oi")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Autor");
    }

    @Test
    void rejectsTextAboveLimit() {
        String text = "x".repeat(RoomMessage.MAX_TEXT + 1);

        assertThatThrownBy(() -> this.message("ana", text)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Texto");
    }

    private RoomMessage message(
            String author,
            String text
    ) {
        return new RoomMessage(
                new Room("geral"),
                author,
                text,
                "instancia-8080",
                NOW
        );
    }
}
