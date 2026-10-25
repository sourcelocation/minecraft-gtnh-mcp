package dev.gtnhmcp.server;

import org.apache.logging.log4j.Logger;

/** The MCP endpoint's lifecycle; the tool catalog outlives it, so the debug screen works while it is off. */
public final class McpService {

    private final ToolCatalog catalog;
    private final ToolAdapter adapter;
    private final String version;
    private final int port;
    private final Logger log;
    private McpHttpServer server;

    McpService(ToolCatalog catalog, String version, int port, Logger log) {
        this.catalog = catalog;
        this.adapter = new ToolAdapter(catalog);
        this.version = version;
        this.port = port;
        this.log = log;
    }

    public ToolCatalog catalog() {
        return catalog;
    }

    public int tools() {
        return catalog.entries()
            .size();
    }

    /** Calls made by MCP clients since the game started; debug-screen runs are not counted. */
    public long calls() {
        return adapter.calls();
    }

    public String url() {
        return "http://127.0.0.1:" + port + McpHttpServer.PATH;
    }

    public synchronized boolean running() {
        return server != null;
    }

    /** Returns false, and logs why, when the port cannot be bound. */
    public synchronized boolean start() {
        if (server != null) return true;
        try {
            server = new McpHttpServer(
                version,
                port,
                catalog.entries()
                    .stream()
                    .map(adapter::adapt)
                    .toList());
            log.info(
                "Serving {} MCP tools on {}",
                catalog.entries()
                    .size(),
                url());
            return true;
        } catch (Exception e) {
            log.error("GTNH MCP could not listen on {}", url(), e);
            return false;
        }
    }

    public synchronized void stop() {
        if (server == null) return;
        try {
            server.close();
        } catch (Exception e) {
            log.warn("GTNH MCP did not stop cleanly", e);
        }
        server = null;
        log.info("GTNH MCP stopped");
    }
}
