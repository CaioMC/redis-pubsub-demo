package io.github.caiomc.pubsub.core.room.domain.model;

import java.time.Instant;

import static java.util.Objects.isNull;

/**
 * Mensagem publicada numa sala.
 *
 * @param origin instância que recebeu o POST e fez o {@code PUBLISH}; mostra que a entrega atravessou o Redis
 */
public record RoomMessage(
        Room room,
        String author,
        String text,
        String origin,
        Instant sentAt
) {

    public static final int MAX_AUTHOR = 40;

    public static final int MAX_TEXT = 500;

    public RoomMessage {
        if (isNull(room)) {
            throw new IllegalArgumentException("A mensagem precisa de uma sala");
        }

        if (isNull(author) || author.isBlank() || author.length() > MAX_AUTHOR) {
            throw new IllegalArgumentException("Autor obrigatório, com até " + MAX_AUTHOR + " caracteres");
        }

        if (isNull(text) || text.isBlank() || text.length() > MAX_TEXT) {
            throw new IllegalArgumentException("Texto obrigatório, com até " + MAX_TEXT + " caracteres");
        }

        if (isNull(origin) || origin.isBlank()) {
            throw new IllegalArgumentException("A mensagem precisa da instância de origem");
        }

        if (isNull(sentAt)) {
            throw new IllegalArgumentException("A mensagem precisa do horário de envio");
        }
    }
}
