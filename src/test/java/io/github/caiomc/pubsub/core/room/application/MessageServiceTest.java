package io.github.caiomc.pubsub.core.room.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.caiomc.pubsub.core.room.application.MessageServiceTestDoubles.LoopbackBroker;
import io.github.caiomc.pubsub.core.room.domain.model.Room;
import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;
import io.github.caiomc.pubsub.core.room.usecase.RoomSubscription;
import io.github.caiomc.pubsub.core.room.usecase.command.PublishMessageCommand;
import io.github.caiomc.pubsub.core.room.usecase.command.SubscribeRoomCommand;

import static org.assertj.core.api.Assertions.assertThat;

class MessageServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    private static final Room GERAL = new Room("geral");

    private LoopbackBroker broker;

    private MessageService service;

    @BeforeEach
    void setUp() {
        this.broker = new LoopbackBroker();
        this.service = new MessageService(
                this.broker,
                new RoomSettings("instancia-a"),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        this.service.listen();
    }

    @Test
    void publishesToBrokerWithOriginAndClock() {
        RoomMessage message = this.service.publish(new PublishMessageCommand(GERAL, "ana", "oi"));

        assertThat(this.broker.published).containsExactly(message);
        assertThat(message.origin()).isEqualTo("instancia-a");
        assertThat(message.sentAt()).isEqualTo(NOW);
    }

    @Test
    void deliversOnlyToSubscribersOfTheSameRoom() {
        List<RoomMessage> geral = new ArrayList<>();
        List<RoomMessage> outra = new ArrayList<>();

        this.service.subscribe(new SubscribeRoomCommand(GERAL, geral::add));
        this.service.subscribe(new SubscribeRoomCommand(new Room("outra"), outra::add));

        this.service.publish(new PublishMessageCommand(GERAL, "ana", "oi"));

        assertThat(geral).hasSize(1);
        assertThat(outra).isEmpty();
    }

    @Test
    void stopsDeliveringAfterCancel() {
        List<RoomMessage> received = new ArrayList<>();
        RoomSubscription subscription = this.service.subscribe(new SubscribeRoomCommand(GERAL, received::add));

        subscription.cancel();
        subscription.cancel();

        this.service.publish(new PublishMessageCommand(GERAL, "ana", "oi"));

        assertThat(received).isEmpty();
    }

    @Test
    void failingSubscriberDoesNotBlockOthers() {
        List<RoomMessage> received = new ArrayList<>();

        this.service.subscribe(new SubscribeRoomCommand(GERAL, message -> {
            throw new IllegalStateException("conexão fechada");
        }));
        this.service.subscribe(new SubscribeRoomCommand(GERAL, received::add));

        this.service.publish(new PublishMessageCommand(GERAL, "ana", "oi"));

        assertThat(received).hasSize(1);
    }
}
