package io.github.caiomc.pubsub.adapters.room.redis;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.pubsub.core.room.domain.model.Room;
import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;
import io.github.caiomc.pubsub.core.room.port.broker.MessageBrokerPort;

/**
 * Um canal por sala ({@code chat:room:<sala>}).
 * <ul>
 *   <li>{@link #publish}: {@code PUBLISH chat:room:<sala> <json>}</li>
 *   <li>{@link #listen}: {@code PSUBSCRIBE chat:room:*}, uma inscrição só por instância, valendo para todas as salas</li>
 * </ul>
 */
@Component
public class RedisMessageBroker implements MessageBrokerPort {

    private static final Logger LOG = LoggerFactory.getLogger(RedisMessageBroker.class);

    static final String CHANNEL_PREFIX = "chat:room:";

    static final String PATTERN = CHANNEL_PREFIX + "*";

    private final StringRedisTemplate redis;

    private final RedisMessageListenerContainer container;

    private final ObjectMapper mapper;

    public RedisMessageBroker(
            StringRedisTemplate redis,
            RedisMessageListenerContainer container,
            ObjectMapper mapper
    ) {
        this.redis = redis;
        this.container = container;
        this.mapper = mapper;
    }

    @Override
    public void publish(RoomMessage message) {
        String channel = CHANNEL_PREFIX + message.room().name();
        Long receivers = this.redis.convertAndSend(channel, this.write(message));

        LOG.debug("PUBLISH {} entregue a {} inscrição(ões)", channel, receivers);
    }

    @Override
    public void listen(Consumer<RoomMessage> listener) {
        this.container.addMessageListener(
                (message, pattern) -> this.receive(message, listener),
                new PatternTopic(PATTERN)
        );
    }

    private void receive(
            Message message,
            Consumer<RoomMessage> listener
    ) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            listener.accept(this.read(body));
        } catch (RuntimeException e) {
            LOG.warn("Mensagem descartada no canal {}: {}", new String(message.getChannel(), StandardCharsets.UTF_8), e.getMessage());
        }
    }

    private String write(RoomMessage message) {
        RedisMessagePayload payload = new RedisMessagePayload(
                message.room().name(),
                message.author(),
                message.text(),
                message.origin(),
                message.sentAt()
        );

        try {
            return this.mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar a mensagem", e);
        }
    }

    private RoomMessage read(String body) {
        try {
            RedisMessagePayload payload = this.mapper.readValue(body, RedisMessagePayload.class);

            return new RoomMessage(
                    new Room(payload.room()),
                    payload.author(),
                    payload.text(),
                    payload.origin(),
                    payload.sentAt()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON inválido: " + body, e);
        }
    }
}
