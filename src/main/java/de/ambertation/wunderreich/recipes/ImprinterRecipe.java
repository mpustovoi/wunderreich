package de.ambertation.wunderreich.recipes;

import de.ambertation.wunderreich.Wunderreich;
import de.ambertation.wunderreich.config.Configs;
import de.ambertation.wunderreich.gui.whisperer.EnchantmentInfo;
import de.ambertation.wunderreich.gui.whisperer.WhisperContainer;
import de.ambertation.wunderreich.gui.whisperer.WhisperRule;
import de.ambertation.wunderreich.registries.WunderreichBlocks;
import de.ambertation.wunderreich.registries.WunderreichRecipes;
import de.ambertation.wunderreich.registries.WunderreichRules;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import com.google.gson.*;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class ImprinterRecipe extends WhisperRule implements Recipe<WhisperContainer.Input> {
    public static final int COST_A_SLOT = 0;
    public static final int COST_B_SLOT = 1;
    private static final List<ImprinterRecipe> RECIPES = new LinkedList<>();
    //    private static List<ImprinterRecipe> RECIPES_UI_SORTED = new LinkedList<>();
    private final ResourceLocation id;

    private ImprinterRecipe(
            ResourceLocation id,
            Enchantment enchantment,
            Ingredient inputA,
            Ingredient inputB,
            int baseXP
    ) {
        super(enchantment, inputA, inputB, baseXP);
        this.id = id;
    }

    private ImprinterRecipe(
            ResourceLocation id,
            Enchantment enchantment,
            Ingredient inputA,
            Ingredient inputB,
            ItemStack output,
            int baseXP,
            ItemStack type
    ) {
        super(enchantment, inputA, inputB, output, baseXP, type);
        this.id = id;
    }

    private ImprinterRecipe(Enchantment e) {
        super(e);

        this.id = makeID(e);
    }

    @NotNull
    private static ResourceLocation makeID(Enchantment e) {
        return Wunderreich.ID(Type.ID.getPath() + "/" + e.getDescriptionId());
    }

    public static List<ImprinterRecipe> getRecipes() {
        return RECIPES;
    }

    public static List<ImprinterRecipe> getUISortedRecipes() {
        return RECIPES
                .stream()
                .sorted(Comparator.comparing(a -> a.getCategory() + ":" + a.getName()))
                .collect(Collectors.toList());
    }

    private static void resortRecipes() {
//        RECIPES_UI_SORTED = RECIPES
//                .stream()
//                .sorted(Comparator.comparing(a -> a.getCategory() + ":" + a.getName()))
//                .collect(Collectors.toList());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Serializer.ID, Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Type.ID, Type.INSTANCE);

        RECIPES.clear();

        if (WunderreichRules.Whispers.allowLibrarianSelection()) {
            List<Enchantment> enchants = new LinkedList<>();
            BuiltInRegistries.ENCHANTMENT
                    .stream()
                    .filter(e -> e.isTradeable())
                    .forEach(e -> {
                        ResourceLocation ID = makeID(e);
                        if (Configs.RECIPE_CONFIG.newBooleanFor(ID.getPath(), ID).get())
                            enchants.add(e);
                    });
            enchants.sort(Comparator.comparing(a -> WhisperRule.getFullname(a)
                                                               .getString()));

            enchants.forEach(e -> {
                ImprinterRecipe r = new ImprinterRecipe(e);
                RECIPES.add(r);
                WunderreichRecipes.RECIPES.put(r.id, Serializer.INSTANCE.toJson(r));
            });
        }

        resortRecipes();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(WunderreichBlocks.WHISPER_IMPRINTER);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, inputA, inputB);
    }


    @Override
    public boolean matches(WhisperContainer.Input inv, Level level) {
        if (inv.size() < 2) return false;
        return this.inputA.test(inv.getItem(COST_A_SLOT)) && this.inputB.test(inv.getItem(COST_B_SLOT)) ||
                this.inputA.test(inv.getItem(COST_B_SLOT)) && this.inputB.test(inv.getItem(COST_A_SLOT));
    }

    @Override
    public ItemStack assemble(WhisperContainer.Input recipeInput, HolderLookup.Provider provider) {
        return this.output.copy();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registryAccess) {
        return this.output;
    }


    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImprinterRecipe)) return false;
        ImprinterRecipe that = (ImprinterRecipe) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Type implements RecipeType<ImprinterRecipe> {
        public static final ResourceLocation ID = Wunderreich.ID("imprinter");
        public static final RecipeType<ImprinterRecipe> INSTANCE = new Type(); //Registry.register(Registry.RECIPE_TYPE, Wunderreich.makeID(ID+"_recipe"), new Type());

        Type() {
        }

        @Override
        public String toString() {
            return ID.toString();
        }
    }

    private static class Serializer implements RecipeSerializer<ImprinterRecipe> {
        public final static ResourceLocation ID = Type.ID;
        public final static Serializer INSTANCE = new Serializer(); //Registry.register(Registry.RECIPE_SERIALIZER, Wunderreich.makeID(Type.ID), new Serializer());

        @Override
        public ImprinterRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf packetBuffer) {
            Ingredient costA = Ingredient.fromNetwork(packetBuffer);
            int count = packetBuffer.readByte();
            if (costA.getItems().length > 0 && count > 0) {
                ItemStack stack = new ItemStack(costA.getItems()[0].getItem(), count);
                costA = Ingredient.of(stack);
            }
            Ingredient costB = Ingredient.fromNetwork(packetBuffer);
            ItemStack output = packetBuffer.readItem();
            /*byte packetType = */
            packetBuffer.readByte(); //this is a type we currently do not use
            ItemStack type = packetBuffer.readItem();
            int baseXP = packetBuffer.readVarInt();

            ResourceLocation eID = packetBuffer.readResourceLocation();
            var enchantment = BuiltInRegistries.ENCHANTMENT.getOptional(eID);

            return new ImprinterRecipe(
                    id,
                    enchantment.orElseThrow(() -> new RuntimeException("Unknown Enchantment " + eID)),
                    costA,
                    costB,
                    output,
                    baseXP,
                    type
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ImprinterRecipe recipe) {
            recipe.inputA.toNetwork(buf);
            buf.writeByte(recipe.inputA.getItems().length == 0 ? 0 : recipe.inputA.getItems()[0].getCount());
            recipe.inputB.toNetwork(buf);
            buf.writeItem(recipe.output);
            buf.writeByte(0); //this is a type we currently do not use
            buf.writeItem(recipe.type);
            buf.writeVarInt(recipe.baseXP);
            buf.writeResourceLocation(EnchantmentHelper.getEnchantmentId(recipe.enchantment));
        }

        public JsonElement toJson(ImprinterRecipe r) {
            ImprinterRecipeJsonFormat recipeJson = new ImprinterRecipeJsonFormat();
            recipeJson.enchantment = BuiltInRegistries.ENCHANTMENT.getKey(r.enchantment).toString();
            recipeJson.xp = r.baseXP;
            recipeJson.inputA = r.inputA.toJson();
            recipeJson.count = r.inputA.getItems().length == 0 ? 0 : r.inputA.getItems()[0].getCount();
            recipeJson.inputB = r.inputB.toJson();
            recipeJson.type = Type.ID.toString();

            JsonObject root = new JsonObject();
            root.add("type", new JsonPrimitive(recipeJson.type));
            root.add("enchantment", new JsonPrimitive(recipeJson.enchantment));
            root.add("xp", new JsonPrimitive(recipeJson.xp));
            root.add("inputA", recipeJson.inputA);
            root.add("count", new JsonPrimitive(recipeJson.count));
            root.add("inputB", recipeJson.inputB);

            return root;
        }

        @Override
        public ImprinterRecipe fromJson(ResourceLocation id, JsonObject json) {
            ImprinterRecipeJsonFormat recipeJson = new Gson().fromJson(json, ImprinterRecipeJsonFormat.class);

            if (recipeJson.inputA == null) {
                throw new JsonSyntaxException("The Attribute 'inputA' is missing.");
            }
            if (recipeJson.inputB == null) {
                throw new JsonSyntaxException("The Attribute 'inputB' is missing.");
            }
            if (recipeJson.enchantment == null) {
                throw new JsonSyntaxException("The Attribute 'output' is missing.");
            }

            ResourceLocation eID = new ResourceLocation(recipeJson.enchantment);
            var enchantment = BuiltInRegistries.ENCHANTMENT.getOptional(eID);
            if (!enchantment.isPresent()) {
                throw new JsonParseException("Unknown Enchantment " + eID);
            }


            Ingredient inputA = Ingredient.fromJson(recipeJson.inputA);
            if (inputA.getItems().length > 0 && recipeJson.count > 0) {
                ItemStack stack = new ItemStack(inputA.getItems()[0].getItem(), recipeJson.count);
                inputA = Ingredient.of(stack);
            }
            Ingredient inputB = Ingredient.fromJson(recipeJson.inputB);

            if (recipeJson.xp <= 0) {
                EnchantmentInfo ei = new EnchantmentInfo(enchantment.get());
                recipeJson.xp = ei.baseXP;
            }


            ImprinterRecipe r = new ImprinterRecipe(id, enchantment.get(), inputA, inputB, recipeJson.xp);

            RECIPES.remove(r);
            RECIPES.add(r);
            resortRecipes();

            return r;
        }

        static class ImprinterRecipeJsonFormat {
            String type;
            JsonElement inputA;
            int count;
            JsonElement inputB;
            int xp;
            String enchantment;
        }
    }
}
