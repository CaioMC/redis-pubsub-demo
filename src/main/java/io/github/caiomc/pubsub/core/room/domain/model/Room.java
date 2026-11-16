package io.github.caiomc.pubsub.core.room.domain.model;

import java.util.regex.Pattern;

import static java.util.Objects.isNull;

/**
 * Nome de uma sala. Vira o sufixo do canal Redis ({@code chat:room:<name>}), por isso só aceita caracteres seguros.
 */
public record Room(String name) {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9-]{1,40}");

    public Room {
        if (isNull(name) || !PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Nome de sala inválido: use de 1 a 40 caracteres entre a-z, 0-9 e hífen");
        }
    }
}
