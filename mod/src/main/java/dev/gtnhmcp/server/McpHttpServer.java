package dev.gtnhmcp.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
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

    static final String PATH = "/mcp";
    private static final String INSTRUCTIONS = "Live data from the player's GT New Horizons client. Item ids from any tool are accepted by every tool; recipe ids last until the game restarts.";
    private static final String WEBSITE = "https://github.com/sourcelocation/minecraft-gtnh-mcp";

    private final Server jetty;

    McpHttpServer(String version, int port, List<SyncToolSpecification> tools) throws Exception {
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
            .serverInfo(
                McpSchema.Implementation.builder("gtnh-mcp", version)
                    .title("GTNH MCP")
                    .websiteUrl(WEBSITE)
                    .icons(icon())
                    .build())
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
        connector.setPort(port);
        jetty.addConnector(connector);
        ServletContextHandler context = new ServletContextHandler();
        context.addServlet(new ServletHolder(transport), PATH);
        jetty.setHandler(context);
        try {
            jetty.start();
        } catch (Exception e) {
            jetty.stop();
            throw e;
        }
    }

    public void close() throws Exception {
        jetty.stop();
    }

    /** The mod icon as a data URI, which clients show next to the server. */
    private static List<McpSchema.Icon> icon() throws IOException {
        try (InputStream png = McpHttpServer.class.getResourceAsStream("/assets/gtnhmcp/icon.png")) {
            if (png == null) return List.of();
            String uri = "data:image/png;base64," + Base64.getEncoder()
                .encodeToString(png.readAllBytes());
            return List.of(
                McpSchema.Icon.builder(uri)
                    .mimeType("image/png")
                    .sizes(List.of("128x128"))
                    .build());
        }
    }
}
