package dev.gtnhmcp.server;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhmcp.core.ToolException;

/** Moves work from HTTP threads onto Minecraft's client thread, where game state is safe to read. */
public final class ClientThread {

    private static final long TIMEOUT_SECONDS = 5;
    private static final int TASKS_PER_TICK = 4;
    private final Queue<FutureTask<?>> pending = new ConcurrentLinkedQueue<>();

    <T> T call(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        pending.add(task);
        try {
            return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            task.cancel(false);
            throw new ToolException("Minecraft did not respond; it may still be loading");
        } catch (ExecutionException e) {
            throw e.getCause() instanceof Exception cause ? cause : e;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (int i = 0; i < TASKS_PER_TICK; i++) {
            FutureTask<?> task = pending.poll();
            if (task == null) return;
            task.run();
        }
    }
}
