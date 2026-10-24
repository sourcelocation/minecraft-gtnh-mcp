package dev.gtnhmcp.server;

import java.util.List;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.jackson3.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

/** Stateless Streamable HTTP on loopback only; the Host check blocks DNS rebinding. */
final class McpHttpServer implements AutoCloseable {

    static final int PORT = 8765;
    static final String PATH = "/mcp";
    private static final String INSTRUCTIONS = "Live data from the player's GT New Horizons client. Item ids from any tool are accepted by every tool; recipe ids last until the game restarts.";

    private final Server jetty;

    McpHttpServer(String version, List<SyncToolSpecification> tools) throws Exception {
        JsonMapper mapper = JsonMapper.builder()
            .build();
        McpJsonMapper json = new JacksonMcpJsonMapper(mapper);
        HttpServletStatelessServerTransport transport = HttpServletStatelessServerTransport.builder()
            .jsonMapper(json)
            .messageEndpoint(PATH)
            .securityValidator(
                DefaultServerTransportSecurityValidator.builder()
                    .allowedOrigins(List.of("http://localhost:*", "http://127.0.0.1:*"))
                    .allowedHosts(List.of("localhost:*", "127.0.0.1:*"))
                    .build())
            .build();
        McpServer.sync(transport)
            .serverInfo("gtnh-mcp", version)
            .instructions(INSTRUCTIONS)
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .tools(false)
                    .build())
            .jsonMapper(json)
            .jsonSchemaValidator(new DefaultJsonSchemaValidator(mapper))
            .tools(tools)
            .build();

        jetty = new Server(new QueuedThreadPool(8, 2));
        ServerConnector connector = new ServerConnector(jetty);
        connector.setHost("127.0.0.1");
        connector.setPort(PORT);
        jetty.addConnector(connector);
        ServletContextHandler context = new ServletContextHandler();
        context.addServlet(new ServletHolder(transport), PATH);
        jetty.setHandler(context);
        jetty.start();
    }

    public void close() throws Exception {
        jetty.stop();
    }
}
