package dev.gtnhmcp.client.debug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.gtnhmcp.client.debug.form.ArgumentForm;
import dev.gtnhmcp.client.debug.form.ArgumentForms;
import dev.gtnhmcp.client.debug.form.InvalidArgument;
import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.server.ToolCatalog;
import dev.gtnhmcp.server.ToolResult;

/**
 * The debug screen's state and behaviour, free of GUI code: a form per tool, the selected tool, and the last run's
 * output. Views subscribe with {@link #onOutputChanged}.
 */
public final class ToolConsole {

    private final ToolCatalog catalog;
    private final Map<ToolCatalog.Entry, ArgumentForm> forms = new LinkedHashMap<>();
    private final List<Runnable> outputListeners = new ArrayList<>();
    private ToolCatalog.Entry selected;
    private String output = "";
    private String summary = "";

    public ToolConsole(ToolCatalog catalog, ArgumentForms argumentForms) {
        this.catalog = catalog;
        for (ToolCatalog.Entry entry : catalog.entries()) forms.put(
            entry,
            argumentForms.of(
                entry.tool()
                    .input()));
        this.selected = catalog.entries()
            .get(0);
    }

    public List<ToolCatalog.Entry> tools() {
        return catalog.entries();
    }

    /** Each tool keeps its own form, so switching tools does not lose what was typed. */
    public ArgumentForm form(ToolCatalog.Entry entry) {
        return forms.get(entry);
    }

    public boolean isSelected(ToolCatalog.Entry entry) {
        return entry == selected;
    }

    public void select(ToolCatalog.Entry entry) {
        selected = entry;
        show("", "");
    }

    /** The selected tool's display title and MCP name, e.g. "Find items (find_items)". */
    public String title() {
        Tool<?, ?> tool = selected.tool();
        return tool.title() + " (" + tool.name() + ")";
    }

    /** The selected tool's description, as the model sees it. */
    public String description() {
        return selected.tool()
            .description();
    }

    public String output() {
        return output;
    }

    public List<String> outputLines() {
        // \R: Jackson's pretty printer uses the platform line separator, \r\n on Windows.
        return output.isEmpty() ? List.of() : List.of(output.split("\\R"));
    }

    public String summary() {
        return summary;
    }

    public void onOutputChanged(Runnable listener) {
        outputListeners.add(listener);
    }

    /** Runs the selected tool exactly as an MCP client would, and reports what the model would receive. */
    public void run() {
        Map<String, Object> arguments;
        try {
            arguments = forms.get(selected)
                .values();
        } catch (InvalidArgument e) {
            show("Error: " + e.getMessage(), "");
            return;
        }
        long start = System.nanoTime();
        ToolResult result = catalog.call(selected, arguments);
        long millis = (System.nanoTime() - start) / 1_000_000;
        show(
            result.fold(ToolResult.Success::pretty, failure -> "Error: " + failure.message()),
            result.fold(success -> size(millis, success.json()), failure -> millis + " ms"));
    }

    private void show(String output, String summary) {
        this.output = output;
        this.summary = summary;
        outputListeners.forEach(Runnable::run);
    }

    /** The model receives the compact JSON; about four characters per token is a fair estimate for it. */
    private static String size(long millis, String json) {
        int chars = json.length();
        return millis + " ms, " + chars + " chars, ~" + chars / 4 + " tokens";
    }
}
