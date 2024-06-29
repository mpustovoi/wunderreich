package de.ambertation.wunderreich.blocks;

import de.ambertation.wunderreich.interfaces.BlockTagSupplier;
import de.ambertation.wunderreich.interfaces.CanDropLoot;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Consumer;

public class AbstractStairBlock extends net.minecraft.world.level.block.StairBlock implements BlockTagSupplier, CanDropLoot {
    public AbstractStairBlock(Block baseBlock) {
        super(baseBlock.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(baseBlock));
    }

    @Override
    public void supplyTags(Consumer<TagKey<Block>> blockTags, Consumer<TagKey<Item>> itemTags) {
        blockTags.accept(BlockTags.STAIRS);
    }
}
