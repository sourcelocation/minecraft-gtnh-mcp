package dev.gtnhmcp.client.debug;

import dev.gtnhmcp.client.debug.form.ArgumentForms;
import dev.gtnhmcp.client.debug.view.ArgumentViews;
import dev.gtnhmcp.server.ToolCatalog;

/** Opens a fresh tool console on top of the current screen, which it returns to when closed. */
public final class ConsoleLauncher {

    private final ToolCatalog catalog;
    private final ArgumentForms forms;
    private final ArgumentViews views;

    public ConsoleLauncher(ToolCatalog catalog, ArgumentForms forms, ArgumentViews views) {
        this.catalog = catalog;
        this.forms = forms;
        this.views = views;
    }

    public void open() {
        DebugScreen.open(new ToolConsole(catalog, forms), views);
    }
}
