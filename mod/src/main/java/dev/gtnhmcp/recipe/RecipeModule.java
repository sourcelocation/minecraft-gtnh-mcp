package dev.gtnhmcp.recipe;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.gtnhmcp.core.AbstractTool;
import dev.gtnhmcp.core.Extension;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Page;
import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.core.ToolException;
import dev.gtnhmcp.core.ToolModule;

public final class RecipeModule implements ToolModule {

    public List<Tool<?, ?>> tools(Game game) {
        RecipeBook book = new RecipeBook();
        RecipeSearch search = new RecipeSearch(book, new RecipeSources());
        RecipeRenderer renderer = new RecipeRenderer(game.items(), book, Extension.load(RecipeDetail.class));
        ChainPlanner planner = new ChainPlanner(game.items(), search, renderer);
        return List.of(
            new FindRecipes(game.items(), search, renderer),
            new GetRecipe(book, renderer),
            new PlanChain(game, planner));
    }

    public record Lookup(@JsonProperty(required = true) @JsonPropertyDescription("item id or NEI search") String item,
        @JsonPropertyDescription("true for recipes that consume the item") Boolean uses,
        @JsonPropertyDescription("only this machine's recipes, e.g. one listed under collapsed") String machine,
        @JsonPropertyDescription("include recycling (parts back to raw material); default false") Boolean recycling,
        String cursor, Integer limit) {}

    public record Recipes(List<RecipeView> items,
        @JsonPropertyDescription("machines whose recipes are only counted; pass one as machine to list them") Map<String, Integer> collapsed,
        @JsonPropertyDescription("pass as cursor for the next page") String next) {}

    public record RecipeRef(@JsonProperty(required = true) String id) {}

    public record Target(@JsonProperty(required = true) String item, Long amount) {}

    public record PlanRequest(@JsonProperty(required = true) List<Target> targets,
        @JsonPropertyDescription("item ids already available; planning stops at them") List<String> have,
        @JsonPropertyDescription("item ids no step may consume") List<String> avoid,
        @JsonPropertyDescription("distinct recipes, default 12, max 64") Integer max_steps) {}

    static final class FindRecipes extends AbstractTool<Lookup, Recipes> {

        private final Items items;
        private final RecipeSearch search;
        private final RecipeRenderer renderer;

        FindRecipes(Items items, RecipeSearch search, RecipeRenderer renderer) {
            super(
                "find_recipes",
                "Find recipes",
                "NEI recipes that make an item, or use it with uses=true. Default limit 10. Recycling is left out, "
                    + "and rare or duplicate sources are only counted under collapsed.");
            this.items = items;
            this.search = search;
            this.renderer = renderer;
        }

        public Recipes call(Lookup q) {
            RecipeSearch.Result result = search.find(
                items.resolve(q.item()),
                Boolean.TRUE.equals(q.uses()),
                q.machine(),
                Boolean.TRUE.equals(q.recycling()));
            Page<RecipeView> page = Page.of(
                result.recipes()
                    .map(renderer::render),
                q.cursor(),
                q.limit() == null ? 10 : q.limit());
            return new Recipes(page.items(), result.collapsed(), page.next());
        }
    }

    static final class GetRecipe extends AbstractTool<RecipeRef, RecipeView> {

        private final RecipeBook book;
        private final RecipeRenderer renderer;

        GetRecipe(RecipeBook book, RecipeRenderer renderer) {
            super("get_recipe", "Recipe", "One recipe by an id from find_recipes or plan_chain.");
            this.book = book;
            this.renderer = renderer;
        }

        public RecipeView call(RecipeRef ref) {
            return renderer.render(book.resolve(ref.id()));
        }
    }

    static final class PlanChain extends AbstractTool<PlanRequest, Plan> {

        private final Game game;
        private final ChainPlanner planner;

        PlanChain(Game game, ChainPlanner planner) {
            super(
                "plan_chain",
                "Plan recipe chain",
                "Plan a recipe chain for targets down to raw materials, preferring supplied and inventory items.");
            this.game = game;
            this.planner = planner;
        }

        public Plan call(PlanRequest request) {
            if (request.targets() == null || request.targets()
                .isEmpty()) throw new ToolException("targets required");
            Items items = game.items();
            Map<ItemStack, Long> targets = new LinkedHashMap<>();
            for (Target target : request.targets())
                targets.merge(items.resolve(target.item()), target.amount() == null ? 1 : target.amount(), Long::sum);
            int steps = request.max_steps() == null ? 12 : Math.max(1, Math.min(request.max_steps(), 64));
            PlanPolicy policy = new PreferAvailable(
                items,
                ids(items, request.have()),
                ids(items, request.avoid()),
                inventory(items));
            return planner.plan(targets, policy, steps);
        }

        private static Set<String> ids(Items items, List<String> refs) {
            return refs == null ? Set.of()
                : refs.stream()
                    .map(r -> items.id(items.resolve(r)))
                    .collect(Collectors.toSet());
        }

        private Set<String> inventory(Items items) {
            Set<String> ids = new HashSet<>();
            try {
                EntityPlayer player = game.player();
                for (ItemStack stack : player.inventory.mainInventory) if (stack != null) ids.add(items.id(stack));
            } catch (ToolException notInWorld) {
                // Planning works without a world; there is just no inventory to prefer.
            }
            return ids;
        }
    }
}
