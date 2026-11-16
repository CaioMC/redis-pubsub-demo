package io.github.caiomc.pubsub.adapters.room.web.sse;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.caiomc.pubsub.adapters.room.web.mapper.RoomWebMapper;
import io.github.caiomc.pubsub.adapters.room.web.response.StreamReadyResponse;
import io.github.caiomc.pubsub.core.room.application.RoomSettings;
import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;
import io.github.caiomc.pubsub.core.room.usecase.RoomSubscription;
import io.github.caiomc.pubsub.core.room.usecase.SubscribeRoomUseCase;

/**
 * Liga uma conexão SSE a uma assinatura da sala. A assinatura é cancelada quando a conexão termina, expira ou falha.
 */
@Component
public class RoomStreams {

    private final SubscribeRoomUseCase subscribe;

    private final RoomWebMapper mapper;

    private final RoomSettings settings;

    public RoomStreams(
            SubscribeRoomUseCase subscribe,
            RoomWebMapper mapper,
            RoomSettings settings
    ) {
        this.subscribe = subscribe;
        this.mapper = mapper;
        this.settings = settings;
    }

    public SseEmitter open(String room) {
        SseEmitter emitter = new SseEmitter(0L);
        RoomSubscription subscription = this.subscribe.subscribe(
                this.mapper.toSubscribeCommand(room, message -> this.send(emitter, message))
        );

        emitter.onCompletion(subscription::cancel);
        emitter.onTimeout(subscription::cancel);
        emitter.onError(error -> subscription.cancel());

        this.ready(emitter, room);

        return emitter;
    }

    private void ready(
            SseEmitter emitter,
            String room
    ) {
        try {
            emitter.send(SseEmitter.event().name("ready").data(new StreamReadyResponse(room, this.settings.instanceId())));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void send(
            SseEmitter emitter,
            RoomMessage message
    ) {
        try {
            emitter.send(SseEmitter.event().name("message").data(this.mapper.toResponse(message)));
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }
}
