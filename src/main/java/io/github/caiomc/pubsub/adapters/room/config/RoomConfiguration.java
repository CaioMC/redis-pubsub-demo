package io.github.caiomc.pubsub.adapters.room.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import io.github.caiomc.pubsub.core.room.application.RoomSettings;

@Configuration
@EnableConfigurationProperties(RoomProperties.class)
class RoomConfiguration {

    @Bean
    RoomSettings roomSettings(RoomProperties properties) {
        return new RoomSettings(properties.instanceId());
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Mantém a conexão dedicada às inscrições: uma conexão em SUBSCRIBE só recebe mensagens, não roda outros comandos.
     */
    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        return container;
    }
}
