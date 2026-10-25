package dev.gtnhmcp.client.debug;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import dev.gtnhmcp.client.debug.view.ArgumentViews;
import dev.gtnhmcp.server.ToolCatalog;

/**
 * Draws a {@link ToolConsole} with ModularUI2 and holds no state of its own. A centred panel: the tool list on the
 * left;
 * on the right the tool's title and description, its argument form, Run and Copy with the result size, and the output
 * in an inset, scrolling box. Closing returns to the screen it was opened from.
 */
public final class DebugScreen {

    private static final float PANEL_SIZE = 0.9f;
    private static final int PADDING = 8;
    private static final int GAP = 6;
    private static final int TOOL_COLUMN_WIDTH = 130;
    private static final int ROW = 16;
    private static final int BUTTON_WIDTH = 50;
    private static final int OUTPUT_PADDING = 4;

    private final ToolConsole console;
    private final ArgumentViews views;
    private final ListWidget<IWidget, ?> output = new ListWidget<>();

    private DebugScreen(ToolConsole console, ArgumentViews views) {
        this.console = console;
        this.views = views;
        console.onOutputChanged(this::showOutput);
    }

    public static void open(ToolConsole console, ArgumentViews views) {
        ModularScreen screen = new ModularScreen("gtnhmcp", new DebugScreen(console, views).panel());
        ClientGUI.open(screen.openParentOnClose(true));
    }

    private ModularPanel panel() {
        Flow body = Flow.row()
            .sizeRel(1f)
            .childPadding(GAP)
            .child(toolColumn())
            .child(toolDetails());
        return ModularPanel.defaultPanel("gtnhmcp_debug")
            .sizeRel(PANEL_SIZE)
            .padding(PADDING)
            .child(body);
    }

    private IWidget toolColumn() {
        ListWidget<IWidget, ?> tools = new ListWidget<>().widthRel(1f)
            .expanded()
            .children(console.tools(), this::toolButton);
        return Flow.column()
            .width(TOOL_COLUMN_WIDTH)
            .heightRel(1f)
            .childPadding(GAP)
            .child(heading(IKey.lang("gtnhmcp.debug.tools")))
            .child(tools);
    }

    private IWidget toolButton(ToolCatalog.Entry entry) {
        String title = entry.tool()
            .title();
        IKey label = IKey.dynamic(() -> console.isSelected(entry) ? "> " + title : title);
        return button(label, () -> console.select(entry)).widthRel(1f);
    }

    /** Sized as "panel minus tool column", not expanded, so the form rows inside can compute their widths. */
    private IWidget toolDetails() {
        return Flow.column()
            .widthRelOffset(1f, -(TOOL_COLUMN_WIDTH + GAP))
            .heightRel(1f)
            .childPadding(GAP)
            .child(heading(IKey.dynamic(console::title)))
            .child(new TextWidget<>(IKey.dynamic(console::description)).widthRel(1f))
            .child(forms())
            .child(actions())
            .child(outputBox());
    }

    /** Every tool's form exists at once and only the selected one is shown, so typed values survive switching. */
    private IWidget forms() {
        Flow forms = Flow.column()
            .widthRel(1f)
            .coverChildrenHeight()
            .collapseDisabledChild();
        for (ToolCatalog.Entry entry : console.tools()) forms.child(
            views.form(console.form(entry))
                .widthRel(1f)
                .setEnabledIf(form -> console.isSelected(entry)));
        return forms;
    }

    private IWidget actions() {
        return Flow.row()
            .widthRel(1f)
            .height(ROW)
            .childPadding(GAP)
            .child(button(IKey.lang("gtnhmcp.debug.run"), console::run).width(BUTTON_WIDTH))
            .child(button(IKey.lang("gtnhmcp.debug.copy"), this::copyOutput).width(BUTTON_WIDTH))
            .child(new TextWidget<>(IKey.dynamic(console::summary)).expanded());
    }

    private IWidget outputBox() {
        return output.widthRel(1f)
            .expanded()
            .padding(OUTPUT_PADDING)
            .background(GuiTextures.DISPLAY);
    }

    /** One widget per line, so the box scrolls through output of any length. */
    private void showOutput() {
        output.removeAll();
        for (String line : console.outputLines()) output.child(
            IKey.str(line)
                .asWidget()
                .widthRel(1f));
        output.scheduleResize();
    }

    private void copyOutput() {
        GuiScreen.setClipboardString(console.output());
    }

    private static IWidget heading(IKey text) {
        return new TextWidget<>(text.style(EnumChatFormatting.BOLD)).widthRel(1f);
    }

    private static ButtonWidget<?> button(IKey label, Runnable action) {
        return new ButtonWidget<>().height(ROW)
            .overlay(label)
            .onMousePressed(mouseButton -> {
                action.run();
                return true;
            });
    }
}
