package dev.gtnhmcp.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.gtnhmcp.core.AbstractTool;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.core.ToolException;
import dev.gtnhmcp.core.ToolModule;

public final class PlayerModule implements ToolModule {

    /** Slots 0-35 are the main inventory, 36-39 armor (boots to helmet). */
    private static final int ARMOR = 36;

    public List<Tool<?, ?>> tools(Game game) {
        return List.of(new GetPlayer(game), new GetSlot(game));
    }

    public record None() {}

    public record Player(String name, int dim, int x, int y, int z,
        @JsonPropertyDescription("0-35 main, 36-39 armor") List<Slot> inventory) {}

    public record Slot(int slot, String id, String name, long amount) {}

    public record SlotRef(@JsonProperty(required = true) @JsonPropertyDescription("0-35 main, 36-39 armor") int slot) {}

    public record SlotDetail(int slot, String id, String name, long amount, List<String> tooltip, List<String> ore,
        String nbt) {}

    static ItemStack[] slots(EntityPlayer player) {
        ItemStack[] all = Arrays.copyOf(player.inventory.mainInventory, ARMOR + player.inventory.armorInventory.length);
        System.arraycopy(player.inventory.armorInventory, 0, all, ARMOR, player.inventory.armorInventory.length);
        return all;
    }

    static final class GetPlayer extends AbstractTool<None, Player> {

        private final Game game;

        GetPlayer(Game game) {
            super("get_player", "Player name, dimension, position and inventory.");
            this.game = game;
        }

        public Player call(None none) {
            EntityPlayer player = game.player();
            Items items = game.items();
            List<Slot> inventory = new ArrayList<>();
            ItemStack[] slots = slots(player);
            for (int i = 0; i < slots.length; i++) if (slots[i] != null)
                inventory.add(new Slot(i, items.id(slots[i]), items.name(slots[i]), slots[i].stackSize));
            return new Player(
                player.getCommandSenderName(),
                player.dimension,
                (int) Math.floor(player.posX),
                (int) Math.floor(player.posY),
                (int) Math.floor(player.posZ),
                inventory);
        }
    }

    static final class GetSlot extends AbstractTool<SlotRef, SlotDetail> {

        private final Game game;

        GetSlot(Game game) {
            super("get_slot", "Full detail of one inventory slot: tooltip, ore names, NBT.");
            this.game = game;
        }

        public SlotDetail call(SlotRef ref) {
            ItemStack[] slots = slots(game.player());
            if (ref.slot() < 0 || ref.slot() >= slots.length)
                throw new ToolException("slot must be 0-" + (slots.length - 1));
            ItemStack stack = slots[ref.slot()];
            if (stack == null) throw new ToolException("slot " + ref.slot() + " is empty");
            Items items = game.items();
            List<String> ores = Arrays.stream(OreDictionary.getOreIDs(stack))
                .mapToObj(OreDictionary::getOreName)
                .toList();
            return new SlotDetail(
                ref.slot(),
                items.id(stack),
                items.name(stack),
                stack.stackSize,
                items.tooltip(stack),
                ores,
                stack.hasTagCompound() ? stack.getTagCompound()
                    .toString() : null);
        }
    }
}
