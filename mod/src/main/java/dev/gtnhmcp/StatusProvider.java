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
        if ("player".equals(method)) return player();
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
        return BridgeResponses.response(
            data,
            minecraft.isSingleplayer() ? "integrated_server" : "client",
            minecraft.isSingleplayer() ? "authoritative" : "observed");
    }

    private JsonObject player() {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        if (player == null) return BridgeResponses.unsupported("player unavailable");
        JsonObject data = new JsonObject();
        data.addProperty("name", player.getCommandSenderName());
        data.addProperty("dimension", player.dimension);
        data.addProperty("x", player.posX);
        data.addProperty("y", player.posY);
        data.addProperty("z", player.posZ);
        data.addProperty("yaw", player.rotationYaw);
        data.addProperty("pitch", player.rotationPitch);
        JsonArray inventory = new JsonArray();
        for (int i = 0; i < player.inventory.mainInventory.length; i++)
            addStack(inventory, i, "main", player.inventory.mainInventory[i]);
        for (int i = 0; i < player.inventory.armorInventory.length; i++)
            addStack(inventory, i, "armor", player.inventory.armorInventory[i]);
        data.add("inventory", inventory);
        return BridgeResponses.response(
            data,
            minecraft.isSingleplayer() ? "integrated_server" : "client",
            minecraft.isSingleplayer() ? "authoritative" : "observed");
    }

    private void addStack(JsonArray inventory, int slot, String scope, ItemStack stack) {
        if (stack == null) return;
        JsonObject entry = new JsonObject();
        entry.addProperty("slot", slot);
        entry.addProperty("scope", scope);
        entry.add("resource", ResourceNormalizer.resource(stack, stack.stackSize));
        inventory.add(entry);
    }
}
