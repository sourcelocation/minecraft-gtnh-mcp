package dev.gtnhmcp.output;

import java.math.BigDecimal;

import dev.gtnhmcp.recipe.RecipeView.Ingredient;

/** A stack with NEI's slot notes after it: {@code 2× Tiny Pile of Gold Dust [...] 25%}, {@code ... kept}. */
final class IngredientLabel extends Label<Ingredient> {

    private final StackLabel stack;

    IngredientLabel(StackLabel stack) {
        super(Ingredient.class);
        this.stack = stack;
    }

    public String describe() {
        return "a stack, then ' <p>%' when chanced, ' any of <n>' when other items also fit, ' kept' when not consumed";
    }

    public String render(Ingredient ingredient) {
        StringBuilder label = new StringBuilder(stack.render(ingredient.stack()));
        if (ingredient.chance() != null) label.append(' ')
            .append(
                BigDecimal.valueOf(ingredient.chance())
                    .movePointRight(2)
                    .stripTrailingZeros()
                    .toPlainString())
            .append('%');
        if (ingredient.alts() != null) label.append(" any of ")
            .append(ingredient.alts());
        if (Boolean.TRUE.equals(ingredient.kept())) label.append(" kept");
        return label.toString();
    }
}
