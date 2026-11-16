package io.github.caiomc.pubsub.core.room.application;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;
import io.github.caiomc.pubsub.core.room.port.broker.MessageBrokerPort;

final class MessageServiceTestDoubles {

    private MessageServiceTestDoubles() {
    }

    /**
     * Imita o Redis: tudo que é publicado volta para todos os ouvintes registrados.
     */
    static final class LoopbackBroker implements MessageBrokerPort {

        final List<RoomMessage> published = new ArrayList<>();

        private final List<Consumer<RoomMessage>> listeners = new ArrayList<>();

        @Override
        public void publish(RoomMessage message) {
            this.published.add(message);
            this.listeners.forEach(listener -> listener.accept(message));
        }

        @Override
        public void listen(Consumer<RoomMessage> listener) {
            this.listeners.add(listener);
        }
    }
}
