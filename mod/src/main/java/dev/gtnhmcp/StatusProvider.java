package dev.gtnhmcp;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

final class StatusProvider implements QueryProvider {

    private final Set<String> capabilities;

    StatusProvider(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

    public JsonObject query(String method, JsonObject params) {
        if ("status".equals(method)) return status();
        if ("player".equals(method)) return player(params);
        if ("player_slot".equals(method)) return playerSlot(params);
        throw new IllegalArgumentException("unknown status method: " + method);
    }

    private JsonObject status() {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        JsonObject data = new JsonObject();
        data.addProperty("side", "client");
        data.addProperty("singleplayer", minecraft.isSingleplayer());
        data.addProperty("connected", minecraft.theWorld != null);
        data.addProperty("player_present", player != null);
        if (player != null) {
            data.addProperty("dimension", player.dimension);
            data.addProperty("player", player.getCommandSenderName());
        }
        JsonArray available = new JsonArray();
        for (String capability : capabilities) available.add(new JsonPrimitive(capability));
        data.add("capabilities", available);
        return data;
    }

    private JsonObject player(JsonObject params) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        if (player == null) return BridgeResponses.error("player unavailable");
        JsonObject data = new JsonObject();
        data.addProperty("name", player.getCommandSenderName());
        data.addProperty("dimension", player.dimension);
        data.addProperty("x", player.posX);
        data.addProperty("y", player.posY);
        data.addProperty("z", player.posZ);
        data.addProperty("yaw", player.rotationYaw);
        data.addProperty("pitch", player.rotationPitch);
        int tooltipLines = integer(params, "tooltip_max_lines", 2, 0, 10);
        JsonArray inventory = new JsonArray();
        for (int i = 0; i < player.inventory.mainInventory.length; i++)
            addStack(inventory, i, "main", player.inventory.mainInventory[i], player, tooltipLines);
        for (int i = 0; i < player.inventory.armorInventory.length; i++)
            addStack(inventory, i, "armor", player.inventory.armorInventory[i], player, tooltipLines);
        data.add("inventory", inventory);
        return data;
    }

    private JsonObject playerSlot(JsonObject params) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return BridgeResponses.error("player unavailable");
        String scope = params.has("scope") ? params.get("scope")
            .getAsString() : "main";
        ItemStack[] inventory;
        if ("main".equals(scope)) inventory = player.inventory.mainInventory;
        else if ("armor".equals(scope)) inventory = player.inventory.armorInventory;
        else return BridgeResponses.error("scope must be main or armor");
        if (!params.has("slot")) return BridgeResponses.error("slot required");
        int slot;
        try {
            slot = params.get("slot")
                .getAsInt();
        } catch (RuntimeException e) {
            return BridgeResponses.error("slot must be an integer");
        }
        if (slot < 0 || slot >= inventory.length) return BridgeResponses.error("slot out of range");
        if (inventory[slot] == null) return BridgeResponses.error("slot is empty");
        JsonObject result = ResourceNormalizer.exactItem(inventory[slot], player);
        result.addProperty("scope", scope);
        result.addProperty("slot", slot);
        return result;
    }

    private void addStack(JsonArray inventory, int slot, String scope, ItemStack stack, EntityPlayer player,
        int tooltipLines) {
        if (stack == null) return;
        JsonObject entry = new JsonObject();
        entry.addProperty("slot", slot);
        entry.addProperty("scope", scope);
        entry.add("item", ResourceNormalizer.resource(stack, stack.stackSize, player, tooltipLines));
        inventory.add(entry);
    }

    private int integer(JsonObject params, String name, int defaultValue, int min, int max) {
        if (!params.has(name)) return defaultValue;
        try {
            int value = params.get(name)
                .getAsInt();
            return value < min || value > max ? defaultValue : value;
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }
}
