package io.github.caiomc.pubsub.adapters.room.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.caiomc.pubsub.adapters.room.web.mapper.RoomWebMapper;
import io.github.caiomc.pubsub.adapters.room.web.request.SendMessageRequest;
import io.github.caiomc.pubsub.adapters.room.web.response.MessageResponse;
import io.github.caiomc.pubsub.adapters.room.web.sse.RoomStreams;
import io.github.caiomc.pubsub.core.room.usecase.PublishMessageUseCase;

@RestController
@RequestMapping("/api/rooms/{room}")
public class RoomController {

    private final PublishMessageUseCase publish;

    private final RoomStreams streams;

    private final RoomWebMapper mapper;

    public RoomController(
            PublishMessageUseCase publish,
            RoomStreams streams,
            RoomWebMapper mapper
    ) {
        this.publish = publish;
        this.streams = streams;
        this.mapper = mapper;
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse send(
            @PathVariable String room,
            @RequestBody(required = false) SendMessageRequest request
    ) {
        return this.mapper.toResponse(
                this.publish.publish(
                        this.mapper.toCommand(room, request)
                )
        );
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String room) {
        return this.streams.open(room);
    }
}
