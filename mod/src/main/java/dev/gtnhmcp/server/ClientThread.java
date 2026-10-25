package dev.gtnhmcp.server;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhmcp.core.ToolException;

/** Runs work on Minecraft's client thread at the end of a tick, where game state is safe to read and GUIs can open. */
public final class ClientThread implements Executor {

    private static final long TIMEOUT_SECONDS = 5;
    private static final int TASKS_PER_TICK = 4;
    private final Queue<FutureTask<?>> pending = new ConcurrentLinkedQueue<>();
    private volatile Thread owner;

    private ClientThread() {}

    public static ClientThread register() {
        ClientThread clientThread = new ClientThread();
        FMLCommonHandler.instance()
            .bus()
            .register(clientThread);
        return clientThread;
    }

    /** Queues work for the next tick, even from the client thread, e.g. a screen that must open after chat closes. */
    public void execute(Runnable work) {
        pending.add(new FutureTask<>(work, null));
    }

    /** Runs work on the client thread and waits for it; inline when already there, e.g. from the debug screen. */
    <T> T call(Callable<T> work) throws Exception {
        if (Thread.currentThread() == owner) return work.call();
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
        owner = Thread.currentThread();
        for (int i = 0; i < TASKS_PER_TICK; i++) {
            FutureTask<?> task = pending.poll();
            if (task == null) return;
            task.run();
        }
    }
}
