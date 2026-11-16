# Diagramas de sequência

<!-- externos: Redis, Navegador, Cliente -->

## 1. Assinar uma sala (SSE)

Cada instância tem **uma** inscrição no Redis (`PSUBSCRIBE chat:room:*`), feita ao subir. As conexões SSE
ficam só em memória, no `MessageService`, separadas por sala.

```mermaid
sequenceDiagram
    participant Navegador
    participant RoomController
    participant RoomStreams
    participant RoomWebMapper
    participant MessageService
    participant RedisMessageBroker
    participant Redis

    Note over MessageService,Redis: ao subir a instância (PostConstruct)
    MessageService->>RedisMessageBroker: listen(this::dispatch)
    RedisMessageBroker->>Redis: PSUBSCRIBE chat:room:*

    Navegador->>RoomController: GET /api/rooms/geral/stream
    RoomController->>RoomStreams: open("geral")
    RoomStreams->>RoomWebMapper: toSubscribeCommand("geral", listener)
    RoomStreams->>MessageService: subscribe(command)
    MessageService-->>RoomStreams: RoomSubscription
    RoomStreams-->>Navegador: event: ready {room, instance}
```

## 2. Publicar uma mensagem e entregar em todas as instâncias

Quem publica não entrega direto aos ouvintes locais: a mensagem sempre faz a volta pelo Redis.

```mermaid
sequenceDiagram
    participant Cliente
    participant RoomController
    participant MessageService
    participant RedisMessageBroker
    participant Redis
    participant RoomStreams
    participant Navegador

    Cliente->>RoomController: POST /api/rooms/geral/messages
    RoomController->>MessageService: publish(PublishMessageCommand)
    MessageService->>RedisMessageBroker: publish(RoomMessage com origin)
    RedisMessageBroker->>Redis: PUBLISH chat:room:geral json
    RoomController-->>Cliente: 202 MessageResponse

    Note over RedisMessageBroker,Navegador: daqui em diante acontece em TODA instância inscrita, inclusive na que publicou
    Redis-->>RedisMessageBroker: pmessage chat:room:* chat:room:geral json
    RedisMessageBroker->>MessageService: dispatch(RoomMessage)
    MessageService->>RoomStreams: listener.accept(message)
    RoomStreams-->>Navegador: event: message
```
