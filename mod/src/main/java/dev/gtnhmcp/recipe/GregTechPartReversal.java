package dev.gtnhmcp.recipe;

import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.Recipe.RecipeIngredient;
import codechicken.nei.recipe.RecipeHandlerRef;
import cpw.mods.fml.common.Loader;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Untagged reversals, e.g. the vanilla furnace smelting an Aluminium Plate back into an ingot: every consumed input is
 * a part GregTech calls recyclable, and every output is the same material's base form. Ores, crushed and purified dusts
 * are not recyclable parts, so real processing is never matched.
 */
public final class GregTechPartReversal implements RecyclingRule {

    private static final Set<OrePrefixes> BASE_FORMS = Set
        .of(OrePrefixes.ingot, OrePrefixes.nugget, OrePrefixes.dust, OrePrefixes.dustSmall, OrePrefixes.dustTiny);

    public boolean available() {
        return Loader.isModLoaded("gregtech");
    }

    public boolean recycles(RecipeHandlerRef ref) {
        Recipe recipe = Recipe.of(ref);
        if (recipe == null) return false;
        List<ItemStack> inputs = recipe.getIngredients()
            .stream()
            .filter(ingredient -> ingredient.getAmount() > 0)
            .map(RecipeIngredient::getItemStack)
            .toList();
        List<ItemStack> outputs = recipe.getResults()
            .stream()
            .map(RecipeIngredient::getItemStack)
            .toList();
        if (inputs.isEmpty() || outputs.isEmpty()) return false;
        Materials material = partMaterial(inputs);
        return material != null && outputs.stream()
            .allMatch(output -> isBaseFormOf(output, material));
    }

    /** The one material all inputs are recyclable parts of, or null. */
    private static Materials partMaterial(List<ItemStack> inputs) {
        Materials material = null;
        for (ItemStack input : inputs) {
            ItemData data = GTOreDictUnificator.getItemData(input);
            if (data == null || data.mMaterial == null) return null;
            if (data.mPrefix != null && !data.mPrefix.isRecyclable()) return null;
            if (material != null && material != data.mMaterial.mMaterial) return null;
            material = data.mMaterial.mMaterial;
        }
        return material;
    }

    private static boolean isBaseFormOf(ItemStack output, Materials material) {
        ItemData data = GTOreDictUnificator.getAssociation(output);
        if (data == null || data.mMaterial == null || !BASE_FORMS.contains(data.mPrefix)) return false;
        Materials made = data.mMaterial.mMaterial;
        return made == material || made == material.mSmeltInto;
    }
}
