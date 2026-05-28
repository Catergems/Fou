package com.fou.item

import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class ChiselItem(settings: Settings) : Item(settings) {

    companion object {
        // Map of base block → ordered list of chiseled variants to cycle through
        val CHISEL_MAP: Map<Block, List<Block>> = mapOf(
            // Stone family
            Blocks.STONE to listOf(
                Blocks.STONE_BRICKS,
                Blocks.CHISELED_STONE_BRICKS,
                Blocks.CRACKED_STONE_BRICKS,
                Blocks.MOSSY_STONE_BRICKS,
                Blocks.STONE
            ),
            // Sandstone family
            Blocks.SANDSTONE to listOf(
                Blocks.CUT_SANDSTONE,
                Blocks.CHISELED_SANDSTONE,
                Blocks.SANDSTONE
            ),
            // Red Sandstone family
            Blocks.RED_SANDSTONE to listOf(
                Blocks.CUT_RED_SANDSTONE,
                Blocks.CHISELED_RED_SANDSTONE,
                Blocks.RED_SANDSTONE
            ),
            // Deepslate family
            Blocks.DEEPSLATE to listOf(
                Blocks.COBBLED_DEEPSLATE,
                Blocks.DEEPSLATE_BRICKS,
                Blocks.CRACKED_DEEPSLATE_BRICKS,
                Blocks.CHISELED_DEEPSLATE,
                Blocks.DEEPSLATE_TILES,
                Blocks.CRACKED_DEEPSLATE_TILES,
                Blocks.POLISHED_DEEPSLATE,
                Blocks.DEEPSLATE
            ),
            // Quartz family
            Blocks.QUARTZ_BLOCK to listOf(
                Blocks.CHISELED_QUARTZ_BLOCK,
                Blocks.QUARTZ_PILLAR,
                Blocks.QUARTZ_BRICKS,
                Blocks.QUARTZ_BLOCK
            ),
            // Andesite family
            Blocks.ANDESITE to listOf(
                Blocks.POLISHED_ANDESITE,
                Blocks.ANDESITE
            ),
            // Diorite family
            Blocks.DIORITE to listOf(
                Blocks.POLISHED_DIORITE,
                Blocks.DIORITE
            ),
            // Granite family
            Blocks.GRANITE to listOf(
                Blocks.POLISHED_GRANITE,
                Blocks.GRANITE
            ),
            // Blackstone family
            Blocks.BLACKSTONE to listOf(
                Blocks.POLISHED_BLACKSTONE,
                Blocks.CHISELED_POLISHED_BLACKSTONE,
                Blocks.POLISHED_BLACKSTONE_BRICKS,
                Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS,
                Blocks.BLACKSTONE
            ),
            // Basalt family
            Blocks.BASALT to listOf(
                Blocks.POLISHED_BASALT,
                Blocks.SMOOTH_BASALT,
                Blocks.BASALT
            ),
            // Copper family
            Blocks.COPPER_BLOCK to listOf(
                Blocks.CUT_COPPER,
                Blocks.CHISELED_COPPER,
                Blocks.COPPER_BLOCK
            ),
            // Brick family
            Blocks.BRICKS to listOf(
                Blocks.BRICKS
            ),
            // Nether Brick family
            Blocks.NETHER_BRICKS to listOf(
                Blocks.CHISELED_NETHER_BRICKS,
                Blocks.CRACKED_NETHER_BRICKS,
                Blocks.NETHER_BRICKS
            ),
            // Tuff family
            Blocks.TUFF to listOf(
                Blocks.POLISHED_TUFF,
                Blocks.TUFF_BRICKS,
                Blocks.CHISELED_TUFF,
                Blocks.CHISELED_TUFF_BRICKS,
                Blocks.TUFF
            )
        )

        // Build reverse map: any variant → its family list
        val REVERSE_MAP: Map<Block, List<Block>> by lazy {
            val map = mutableMapOf<Block, List<Block>>()
            CHISEL_MAP.forEach { (_, variants) ->
                variants.forEach { block -> map[block] = variants }
            }
            map
        }
    }

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos   = context.blockPos
        val player = context.player ?: return ActionResult.PASS

        if (world.isClient) return ActionResult.SUCCESS

        val currentBlock = world.getBlockState(pos).block
        val variants = REVERSE_MAP[currentBlock]

        if (variants == null) {
            player.sendMessage(Text.literal("§cThis block can't be chiseled."), true)
            return ActionResult.FAIL
        }

        val currentIndex = variants.indexOf(currentBlock)
        val nextBlock    = variants[(currentIndex + 1) % variants.size]

        world.setBlockState(pos, nextBlock.defaultState)
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_HIT, SoundCategory.BLOCKS, 1.0f, 1.4f)

        if (!player.isCreative) {
            context.stack.damage(1, player, context.hand)
        }

        player.sendMessage(Text.literal("§7Chiseled to §f${nextBlock.name.string}"), true)
        return ActionResult.SUCCESS
    }
}
