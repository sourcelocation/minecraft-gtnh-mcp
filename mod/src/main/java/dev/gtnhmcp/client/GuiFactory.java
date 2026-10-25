package dev.gtnhmcp.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;

import cpw.mods.fml.client.config.DummyConfigElement.DummyCategoryElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.IConfigElement;
import dev.gtnhmcp.client.debug.ConsoleLauncher;

/**
 * The Config screen behind Mods and Esc → Mod Options: GTNHLib's generated settings plus a Tool console button. Forge
 * creates this class and its entries reflectively, so the console launcher is handed over through {@link #install}.
 */
public final class GuiFactory implements SimpleGuiFactory {

    private static ConsoleLauncher console;

    public static void install(ConsoleLauncher launcher) {
        console = launcher;
    }

    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return Screen.class;
    }

    public static final class Screen extends GuiConfig {

        public Screen(GuiScreen parent) throws ConfigException {
            super(parent, elements(), "gtnhmcp", false, false, "GTNH MCP");
        }

        @SuppressWarnings("rawtypes")
        private static List<IConfigElement> elements() throws ConfigException {
            List<IConfigElement> elements = new ArrayList<>(
                ConfigurationManager.getConfigElements(McpConfig.class, false));
            if (console != null)
                elements.add(new DummyCategoryElement("console", "gtnhmcp.config.console", ConsoleEntry.class));
            return elements;
        }
    }

    /** A category-style button that opens the tool console instead of a sub-config screen. */
    public static final class ConsoleEntry extends GuiConfigEntries.CategoryEntry {

        @SuppressWarnings("rawtypes")
        public ConsoleEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement element) {
            super(owningScreen, owningEntryList, element);
        }

        protected GuiScreen buildChildScreen() {
            return null;
        }

        public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (!btnSelectCategory.mousePressed(mc, x, y)) return false;
            btnSelectCategory.func_146113_a(mc.getSoundHandler());
            console.open();
            return true;
        }
    }
}
