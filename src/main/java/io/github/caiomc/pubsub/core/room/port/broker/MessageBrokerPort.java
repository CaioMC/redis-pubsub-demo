package io.github.caiomc.pubsub.core.room.port.broker;

import java.util.function.Consumer;

import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;

/**
 * Barramento entre instâncias. Toda instância que chamou {@link #listen} recebe toda mensagem publicada,
 * inclusive a que publicou. Sem persistência: quem não estava ouvindo perde a mensagem.
 */
public interface MessageBrokerPort {

    void publish(RoomMessage message);

    void listen(Consumer<RoomMessage> listener);
}
