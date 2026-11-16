package io.github.caiomc.pubsub.core.room.application;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.pubsub.core.room.domain.model.Room;
import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;
import io.github.caiomc.pubsub.core.room.port.broker.MessageBrokerPort;
import io.github.caiomc.pubsub.core.room.usecase.PublishMessageUseCase;
import io.github.caiomc.pubsub.core.room.usecase.RoomSubscription;
import io.github.caiomc.pubsub.core.room.usecase.SubscribeRoomUseCase;
import io.github.caiomc.pubsub.core.room.usecase.command.PublishMessageCommand;
import io.github.caiomc.pubsub.core.room.usecase.command.SubscribeRoomCommand;

import jakarta.annotation.PostConstruct;

import static java.util.Objects.isNull;

/**
 * Publicar não entrega direto aos ouvintes locais: a mensagem vai ao broker e volta por {@link #dispatch}
 * em todas as instâncias, inclusive nesta. Assim o caminho é o mesmo para quem está aqui ou em outra instância.
 */
@Service
public class MessageService implements PublishMessageUseCase, SubscribeRoomUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(MessageService.class);

    private final MessageBrokerPort broker;

    private final RoomSettings settings;

    private final Clock clock;

    private final Map<Room, Set<Consumer<RoomMessage>>> listeners = new ConcurrentHashMap<>();

    public MessageService(
            MessageBrokerPort broker,
            RoomSettings settings,
            Clock clock
    ) {
        this.broker = broker;
        this.settings = settings;
        this.clock = clock;
    }

    @PostConstruct
    void listen() {
        this.broker.listen(this::dispatch);
    }

    @Override
    public RoomMessage publish(PublishMessageCommand command) {
        RoomMessage message = new RoomMessage(
                command.room(),
                command.author(),
                command.text(),
                this.settings.instanceId(),
                this.clock.instant()
        );

        this.broker.publish(message);
        LOG.info("Publicada na sala {} por {}", message.room().name(), message.author());

        return message;
    }

    @Override
    public RoomSubscription subscribe(SubscribeRoomCommand command) {
        Set<Consumer<RoomMessage>> roomListeners = this.listeners.computeIfAbsent(command.room(), room -> new CopyOnWriteArraySet<>());

        roomListeners.add(command.listener());
        LOG.info("Nova assinatura na sala {} ({} nesta instância)", command.room().name(), roomListeners.size());

        return () -> roomListeners.remove(command.listener());
    }

    void dispatch(RoomMessage message) {
        Set<Consumer<RoomMessage>> roomListeners = this.listeners.get(message.room());

        if (isNull(roomListeners)) {
            return;
        }

        for (Consumer<RoomMessage> listener : roomListeners) {
            try {
                listener.accept(message);
            } catch (RuntimeException e) {
                LOG.debug("Ouvinte da sala {} falhou: {}", message.room().name(), e.getMessage());
            }
        }
    }
}
