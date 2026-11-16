package io.github.caiomc.pubsub.adapters.room.redis;

import java.time.Instant;

/**
 * Formato JSON que trafega no canal Redis. Separado do domínio para o contrato entre instâncias não mudar por acidente.
 */
record RedisMessagePayload(
        String room,
        String author,
        String text,
        String origin,
        Instant sentAt
) {
}
