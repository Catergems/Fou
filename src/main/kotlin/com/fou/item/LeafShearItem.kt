package com.fou.item

import net.minecraft.block.BlockState
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class LeafShearItem(settings: Settings) : Item(settings) {

    companion object {
        private const val LEAF_LIMIT = 1024
        private val IS_SHEARING = ThreadLocal.withInitial { false }
    }

    override fun postMine(
        stack: ItemStack,
        world: World,
        state: BlockState,
        pos: BlockPos,
        miner: LivingEntity
    ): Boolean {
        if (IS_SHEARING.get()) return super.postMine(stack, world, state, pos, miner)
        if (world.isClient || miner !is ServerPlayerEntity || world !is ServerWorld)
            return super.postMine(stack, world, state, pos, miner)
        if (!state.isIn(BlockTags.LEAVES)) return super.postMine(stack, world, state, pos, miner)

        IS_SHEARING.set(true)
        try {
            floodShear(world, pos, state, miner)
        } finally {
            IS_SHEARING.set(false)
        }

        return super.postMine(stack, world, state, pos, miner)
    }

    private fun floodShear(
        world: ServerWorld,
        origin: BlockPos,
        originState: BlockState,
        player: ServerPlayerEntity
    ) {
        val visited = mutableSetOf<BlockPos>()
        val queue   = ArrayDeque<BlockPos>()
        queue.add(origin)
        visited.add(origin)

        while (queue.isNotEmpty() && visited.size <= LEAF_LIMIT) {
            val current = queue.removeFirst()
            for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
                if (dx == 0 && dy == 0 && dz == 0) continue
                val neighbour = current.add(dx, dy, dz)
                if (neighbour in visited) continue
                val nState = world.getBlockState(neighbour)
                if (nState.block == originState.block && nState.isIn(BlockTags.LEAVES)) {
                    visited.add(neighbour)
                    queue.add(neighbour)
                }
            }
        }

        // Drop leaf blocks directly — silk touch style
        visited.filter { it != origin }.forEach { leafPos ->
            val leafState = world.getBlockState(leafPos)
            world.removeBlock(leafPos, false)
            // Spawn the leaf block as an item drop
            net.minecraft.block.Block.dropStack(
                world,
                leafPos,
                ItemStack(leafState.block.asItem())
            )
        }

        player.sendMessage(Text.literal("§aSheared §f${visited.size} §aleaf blocks."), true)
    }
}
