package io.github.caiomc.pubsub.core.room.usecase;

/**
 * Assinatura local de uma sala. Cancelar é idempotente.
 */
public interface RoomSubscription {

    void cancel();
}
