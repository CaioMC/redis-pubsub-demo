package io.github.caiomc.pubsub.adapters.room.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.caiomc.pubsub.core.room.domain.model.Room;
import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;
import io.github.caiomc.pubsub.core.room.usecase.PublishMessageUseCase;
import io.github.caiomc.pubsub.core.room.usecase.RoomSubscription;
import io.github.caiomc.pubsub.core.room.usecase.SubscribeRoomUseCase;
import io.github.caiomc.pubsub.core.room.usecase.command.PublishMessageCommand;
import io.github.caiomc.pubsub.core.room.usecase.command.SubscribeRoomCommand;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Passa pelo Redis de verdade: o PUBLISH sai pelo template e volta pelo PSUBSCRIBE do listener container.
 */
@SpringBootTest(properties = "demo.room.instance-id=instancia-it")
@Testcontainers(disabledWithoutDocker = true)
class RedisMessageBrokerIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    private PublishMessageUseCase publish;

    @Autowired
    private SubscribeRoomUseCase subscribe;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void messageRoundTripsThroughRedis() throws InterruptedException {
        BlockingQueue<RoomMessage> received = new LinkedBlockingQueue<>();
        RoomSubscription subscription = this.subscribe.subscribe(new SubscribeRoomCommand(new Room("geral"), received::add));

        RoomMessage sent = this.publishUntilDelivered(new Room("geral"), received);

        assertThat(sent.origin()).isEqualTo("instancia-it");

        subscription.cancel();
    }

    @Test
    void messageFromAnotherProcessArrives() throws InterruptedException {
        BlockingQueue<RoomMessage> received = new LinkedBlockingQueue<>();
        RoomSubscription subscription = this.subscribe.subscribe(new SubscribeRoomCommand(new Room("externa"), received::add));
        String json = "{\"room\":\"externa\",\"author\":\"redis-cli\",\"text\":\"oi\",\"origin\":\"outra-instancia\",\"sentAt\":\"" + Instant.now() + "\"}";
        RoomMessage message = null;
        Instant deadline = Instant.now().plus(TIMEOUT);

        while (Instant.now().isBefore(deadline) && isNull(message)) {
            this.redis.convertAndSend("chat:room:externa", json);
            message = received.poll(200, TimeUnit.MILLISECONDS);
        }

        assertThat(message).isNotNull();
        assertThat(message.origin()).isEqualTo("outra-instancia");

        subscription.cancel();
    }

    @Test
    void otherRoomsDoNotReceive() throws InterruptedException {
        BlockingQueue<RoomMessage> geral = new LinkedBlockingQueue<>();
        BlockingQueue<RoomMessage> silenciosa = new LinkedBlockingQueue<>();
        RoomSubscription first = this.subscribe.subscribe(new SubscribeRoomCommand(new Room("geral"), geral::add));
        RoomSubscription second = this.subscribe.subscribe(new SubscribeRoomCommand(new Room("silenciosa"), silenciosa::add));

        this.publishUntilDelivered(new Room("geral"), geral);

        assertThat(silenciosa.poll(300, TimeUnit.MILLISECONDS)).isNull();

        first.cancel();
        second.cancel();
    }

    /**
     * O PSUBSCRIBE do container é assíncrono: o primeiro PUBLISH pode chegar antes da inscrição e se perder, que é
     * justamente o comportamento do Pub/Sub. Por isso publica de novo até receber.
     */
    private RoomMessage publishUntilDelivered(
            Room room,
            BlockingQueue<RoomMessage> received
    ) throws InterruptedException {
        Instant deadline = Instant.now().plus(TIMEOUT);

        while (Instant.now().isBefore(deadline)) {
            RoomMessage sent = this.publish.publish(new PublishMessageCommand(room, "ana", "oi"));
            RoomMessage message = received.poll(200, TimeUnit.MILLISECONDS);

            if (sent.equals(message)) {
                return sent;
            }
        }

        throw new AssertionError("Mensagem não voltou do Redis em " + TIMEOUT);
    }
}
