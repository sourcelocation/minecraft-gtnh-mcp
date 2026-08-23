package dev.gtnhmcp;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

/** Transfers bounded read-only work from the HTTP executor to Minecraft's client thread. */
public final class ClientDispatcher {

    private final ConcurrentLinkedQueue<FutureTask<?>> pending = new ConcurrentLinkedQueue<FutureTask<?>>();

    public <T> T call(Callable<T> c, long timeoutMs) throws Exception {
        FutureTask<T> task = new FutureTask<T>(c);
        pending.add(task);
        try {
            return task.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pending.remove(task);
            task.cancel(false);
            throw e;
        }
    }

    @SubscribeEvent
    public void tick(ClientTickEvent event) {
        if (event.phase != Phase.END) return;
        for (int i = 0; i < 8; i++) {
            FutureTask<?> task = pending.poll();
            if (task == null) return;
            task.run();
        }
    }
}
