# redis-pubsub-demo

Estudo do **Pub/Sub do Redis** com Spring Boot: salas de chat em que a mensagem enviada para **uma**
instância chega aos navegadores conectados em **qualquer** instância.

## A ideia em 30 segundos

```
          POST /messages                                  GET /stream (SSE)
Cliente ─────────────────▶ instância :8080                instância :8081 ◀──── Navegador
                              │ PUBLISH chat:room:geral        ▲
                              ▼                                │ pmessage
                           ┌──────────── Redis ────────────────┘
                           │ PSUBSCRIBE chat:room:*  (cada instância, uma vez)
                           └───────────────────────────────────▶ instância :8080 (também recebe)
```

- **Um canal por sala**: `chat:room:<sala>`.
- **Uma inscrição por instância**: `PSUBSCRIBE chat:room:*` cobre todas as salas. Quem decide para qual
  navegador entregar é o `MessageService`, em memória.
- **Quem publica também recebe**: o POST não entrega direto aos ouvintes locais. A mensagem dá a volta
  pelo Redis, então o caminho é o mesmo para todos.

## O que o Pub/Sub do Redis garante (e o que não garante)

| | |
|---|---|
| Entrega | Na hora, para quem está inscrito **naquele momento** |
| Persistência | Nenhuma. Sem inscritos, a mensagem some |
| Garantia | *At-most-once*: sem ack e sem reenvio. Quem caiu e voltou perde o intervalo |
| Distribuição | *Fan-out*: todo inscrito recebe tudo. Não divide carga como uma fila |

Para histórico ou replay, use **Redis Streams** (`XADD`/`XRANGE`/`XREADGROUP`). O `dta-streaming-poc`
combina os dois: Stream para o replay e Pub/Sub para o tempo real.

## Rodando

Requisitos: Java 21+, Maven, Docker.

```bash
docker compose up -d                 # Redis em 127.0.0.1:6379

PORT=8080 mvn spring-boot:run        # terminal 1
PORT=8081 mvn spring-boot:run        # terminal 2
```

> Já tem outro Redis na 6379? Use outra porta nos três comandos:
> `REDIS_PORT=6380 docker compose up -d` e `REDIS_PORT=6380 PORT=8080 mvn spring-boot:run`.

Assine a sala na instância **8081**:

```bash
curl -N http://127.0.0.1:8081/api/rooms/geral/stream
# event:ready
# data:{"room":"geral","instance":"instancia-8081"}
```

Publique pela instância **8080**:

```bash
curl -X POST http://127.0.0.1:8080/api/rooms/geral/messages \
     -H 'Content-Type: application/json' \
     -d '{"author":"ana","text":"oi do 8080"}'
```

O terminal do `curl -N` recebe a mensagem com `"origin":"instancia-8080"`: ela atravessou o Redis.

### Espiando o Redis

```bash
docker exec -it redis-pubsub-demo redis-cli PSUBSCRIBE 'chat:room:*'   # vê todo o tráfego
docker exec -it redis-pubsub-demo redis-cli PUBSUB NUMPAT               # 1 por instância no ar

# publicando "de fora", como se fosse outra aplicação:
docker exec -it redis-pubsub-demo redis-cli PUBLISH chat:room:geral \
  '{"room":"geral","author":"cli","text":"oi","origin":"redis-cli","sentAt":"2026-11-16T12:00:00Z"}'
```

### Experimento: a mensagem perdida

1. Derrube a instância 8081 (Ctrl+C).
2. Publique duas mensagens pela 8080.
3. Suba a 8081 de novo e assine a sala: as duas mensagens **não** aparecem. Isso é o Pub/Sub
   funcionando como projetado.

## Estrutura

```
core/room/
├── domain/model/        Room, RoomMessage
├── port/broker/         MessageBrokerPort (publish / listen)
├── usecase/             PublishMessageUseCase, SubscribeRoomUseCase, RoomSubscription
│   └── command/         PublishMessageCommand, SubscribeRoomCommand
└── application/         MessageService (registro de ouvintes por sala), RoomSettings
adapters/room/
├── redis/               RedisMessageBroker (PUBLISH / PSUBSCRIBE), RedisMessagePayload
├── web/controller/      RoomController
├── web/sse/             RoomStreams (SseEmitter ⇄ RoomSubscription)
├── web/mapper/          RoomWebMapper
├── web/request|response DTOs
├── web/handler/         RoomExceptionHandler (ProblemDetail)
└── config/              RoomConfiguration (listener container, Clock, RoomSettings)
```

Os fluxos com as classes reais estão em [docs/sequencias.md](docs/sequencias.md).

## Testes

```bash
mvn verify
```

- Unitários: domínio, `MessageService` com um broker de mentira que devolve o que recebe, mapper e controller.
- `RedisMessageBrokerIT`: Redis real via Testcontainers. Faz o round-trip pelo Redis, recebe um `PUBLISH`
  vindo de fora e confere que uma sala não recebe mensagens de outra.

> `src/test/resources/docker-java.properties` fixa `api.version=1.44`: o Testcontainers 1.21.3 usa por padrão
> a API 1.32, que o Docker atual recusa.
