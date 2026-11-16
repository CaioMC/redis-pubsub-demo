package io.github.caiomc.pubsub.adapters.room.web.controller;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.caiomc.pubsub.adapters.room.web.mapper.RoomWebMapper;
import io.github.caiomc.pubsub.adapters.room.web.sse.RoomStreams;
import io.github.caiomc.pubsub.core.room.domain.model.Room;
import io.github.caiomc.pubsub.core.room.domain.model.RoomMessage;
import io.github.caiomc.pubsub.core.room.usecase.PublishMessageUseCase;
import io.github.caiomc.pubsub.core.room.usecase.command.PublishMessageCommand;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
@Import(RoomWebMapper.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PublishMessageUseCase publish;

    @MockitoBean
    private RoomStreams streams;

    @Test
    void publishesMessage() throws Exception {
        RoomMessage message = new RoomMessage(new Room("geral"), "ana", "oi", "instancia-a", Instant.parse("2026-11-16T12:00:00Z"));

        when(this.publish.publish(new PublishMessageCommand(new Room("geral"), "ana", "oi"))).thenReturn(message);

        this.mvc
                .perform(post("/api/rooms/geral/messages").contentType(MediaType.APPLICATION_JSON).content("{\"author\":\"ana\",\"text\":\"oi\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.origin").value("instancia-a"))
                .andExpect(jsonPath("$.text").value("oi"));
    }

    @Test
    void rejectsInvalidRoomWithProblemDetail() throws Exception {
        this.mvc
                .perform(post("/api/rooms/Sala*/messages").contentType(MediaType.APPLICATION_JSON).content("{\"author\":\"ana\",\"text\":\"oi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Nome de sala inválido: use de 1 a 40 caracteres entre a-z, 0-9 e hífen"));

        verify(this.publish, never()).publish(any());
    }
}
