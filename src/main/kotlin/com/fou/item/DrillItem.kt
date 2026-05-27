package com.fou.item

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ToolMaterial
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.ItemTags
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class DrillItem(settings: Settings) : Item(
    ToolMaterial(
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
        1800,
        12.0f,
        0.0f,
        10,
        ItemTags.DIAMOND_TOOL_MATERIALS
    ).applyToolSettings(
        settings,
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
        0.0f,
        -2.8f,
        0.0f
    )
) {
    companion object {
        private const val VEIN_LIMIT = 32
        // Prevents postMine from recursing when breaking extra blocks
        private val IS_BREAKING = ThreadLocal.withInitial { false }
    }

    override fun getMiningSpeed(stack: ItemStack, state: BlockState): Float = 12.0f

    override fun postMine(
        stack: ItemStack,
        world: World,
        state: BlockState,
        pos: BlockPos,
        miner: LivingEntity
    ): Boolean {
        // Guard: skip if we're already inside a drill break cycle
        if (IS_BREAKING.get()) return super.postMine(stack, world, state, pos, miner)
        if (world.isClient || miner !is ServerPlayerEntity) return super.postMine(stack, world, state, pos, miner)

        IS_BREAKING.set(true)
        try {
            if (state.isIn(ConventionalBlockTags.ORES)) {
                veinMine(world, pos, state, miner)
            } else {
                val facing = getActualFacing(miner)
                get3x3Positions(pos, facing).forEach { affectedPos ->
                    val affectedState = world.getBlockState(affectedPos)
                    if (affectedState.isIn(ConventionalBlockTags.ORES)) return@forEach
                    if (affectedState.isIn(BlockTags.FEATURES_CANNOT_REPLACE)) return@forEach
                    if (affectedState.isAir || affectedState.getHardness(world, affectedPos) < 0f) return@forEach
                    miner.interactionManager.tryBreakBlock(affectedPos)
                }
            }
        } finally {
            IS_BREAKING.set(false)
        }

        return super.postMine(stack, world, state, pos, miner)
    }

    private fun veinMine(world: World, origin: BlockPos, originState: BlockState, miner: ServerPlayerEntity) {
        val visited = mutableSetOf<BlockPos>()
        val queue   = ArrayDeque<BlockPos>()
        queue.add(origin)
        visited.add(origin)

        while (queue.isNotEmpty() && visited.size <= VEIN_LIMIT) {
            val current = queue.removeFirst()
            Direction.entries.forEach { dir ->
                val neighbour = current.offset(dir)
                if (neighbour !in visited) {
                    val nState = world.getBlockState(neighbour)
                    if (nState.block == originState.block && nState.isIn(ConventionalBlockTags.ORES)) {
                        visited.add(neighbour)
                        queue.add(neighbour)
                    }
                }
            }
        }

        visited.filter { it != origin }.forEach { miner.interactionManager.tryBreakBlock(it) }
    }

    private fun getActualFacing(miner: ServerPlayerEntity): Direction = when {
        miner.pitch > 45f  -> Direction.DOWN
        miner.pitch < -45f -> Direction.UP
        else               -> miner.horizontalFacing
    }

    private fun get3x3Positions(center: BlockPos, facing: Direction): List<BlockPos> {
        val positions = mutableListOf<BlockPos>()
        for (a in -1..1) {
            for (b in -1..1) {
                if (a == 0 && b == 0) continue
                positions.add(
                    when (facing.axis) {
                        Direction.Axis.Z -> BlockPos(center.x + a, center.y + b, center.z)
                        Direction.Axis.X -> BlockPos(center.x, center.y + b, center.z + a)
                        Direction.Axis.Y -> BlockPos(center.x + a, center.y, center.z + b)
                    }
                )
            }
        }
        return positions
    }
}
