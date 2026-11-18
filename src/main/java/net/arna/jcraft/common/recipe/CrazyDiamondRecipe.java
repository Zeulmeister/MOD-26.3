package net.arna.jcraft.common.recipe;

import lombok.NonNull;
import net.arna.jcraft.api.registry.JRecipeRegistry;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class CrazyDiamondRecipe extends SingleItemRecipe {

    public CrazyDiamondRecipe(final Recipe.CommonInfo commonInfo, final @NonNull Ingredient ingredient, final @NonNull ItemStackTemplate result) {
        super(commonInfo, ingredient, result);
    }

    @Override
    public boolean matches(final @NonNull SingleRecipeInput input, final @NonNull Level level) {
        if (level.isClientSide()) {
            return false;
        }
        return this.input().test(input.item());
    }

    @Override
    public @NonNull RecipeSerializer<? extends SingleItemRecipe> getSerializer() {
        return JRecipeRegistry.CD_SERIALIZER.get();
    }

    @Override
    public @NonNull RecipeType<? extends SingleItemRecipe> getType() {
        return JRecipeRegistry.CD_TYPE.get();
    }

    @Override
    public @NonNull String group() {
        return "";
    }

    @Override
    public @NonNull RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
