package com.fou.block

import com.fou.blockentity.CrusherBlockEntity
import com.fou.registry.ModBlockEntities
import com.mojang.serialization.MapCodec
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class CrusherBlock(settings: Settings) : BlockWithEntity(settings) {

    companion object {
        val FACING = Properties.HORIZONTAL_FACING
    }

    init {
        defaultState = stateManager.defaultState.with(FACING, Direction.NORTH)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState =
        defaultState.with(FACING, ctx.horizontalPlayerFacing.opposite)

    override fun rotate(state: BlockState, rotation: BlockRotation): BlockState =
        state.with(FACING, rotation.rotate(state.get(FACING)))

    override fun mirror(state: BlockState, mirror: BlockMirror): BlockState =
        state.rotate(mirror.getRotation(state.get(FACING)))

    override fun getCodec(): MapCodec<out BlockWithEntity> = createCodec(::CrusherBlock)
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CrusherBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (world.isClient) return null
        return validateTicker(type, ModBlockEntities.CRUSHER, CrusherBlockEntity::tick)
    }

    override fun onUse(
        state: BlockState, world: World, pos: BlockPos,
        player: PlayerEntity, hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.PASS
        val be = world.getBlockEntity(pos) as? CrusherBlockEntity ?: return ActionResult.PASS
        val hand = net.minecraft.util.Hand.MAIN_HAND
        val handStack = player.getStackInHand(hand)

        // Sneak + RMB → retrieve output
        if (player.isSneaking) {
            val output = be.inventory[1]
            if (!output.isEmpty) {
                if (!player.inventory.insertStack(output)) player.dropItem(output, false)
                be.inventory[1] = ItemStack.EMPTY
                be.markDirty()
                player.sendMessage(Text.literal("§eOutput retrieved."), true)
            } else {
                // Also retrieve input if output is empty
                val input = be.inventory[0]
                if (!input.isEmpty) {
                    if (!player.inventory.insertStack(input)) player.dropItem(input, false)
                    be.inventory[0] = ItemStack.EMPTY
                    be.markDirty()
                    player.sendMessage(Text.literal("§eInput retrieved."), true)
                } else {
                    player.sendMessage(Text.literal("§7Crusher is empty."), true)
                }
            }
            return ActionResult.SUCCESS
        }

        // RMB with item → insert to input slot
        if (!handStack.isEmpty && be.inventory[0].isEmpty && isCrushable(handStack)) {
            be.inventory[0] = handStack.copyWithCount(1)
            handStack.decrement(1)
            be.markDirty()
            player.sendMessage(Text.literal("§aItem inserted for crushing!"), true)
            return ActionResult.SUCCESS
        }

        // Show status
        val inputName  = if (be.inventory[0].isEmpty) "§7(empty)" else be.inventory[0].name.string
        val outputName = if (be.inventory[1].isEmpty) "§7(empty)" else be.inventory[1].name.string
        val progressPct = if (be.inventory[0].isEmpty) "§7idle" else "§e${(be.progress * 100 / CrusherBlockEntity.CRUSH_TIME)}%"
        val power = if (be.inputWatts >= CrusherBlockEntity.MIN_WATTS) "§a${be.inputWatts.toInt()}W" else "§cNo power"
        player.sendMessage(
            Text.literal("§6[Crusher] §f$power §7| Input: §f$inputName §7| Output: §f$outputName §7| Progress: $progressPct"), true
        )
        return ActionResult.SUCCESS
    }

    private fun isCrushable(stack: ItemStack): Boolean =
        stack.isOf(net.minecraft.item.Items.STONE)
        || stack.isOf(net.minecraft.item.Items.COBBLESTONE)
        || stack.isOf(net.minecraft.item.Items.GRAVEL)
        || stack.isOf(net.minecraft.item.Items.SAND)

    override fun onStateReplaced(state: BlockState, world: ServerWorld, pos: BlockPos, moved: Boolean) {
        if (!moved) {
            val be = world.getBlockEntity(pos) as? CrusherBlockEntity
            be?.inventory?.forEach { stack ->
                if (!stack.isEmpty) ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
            }
        }
        super.onStateReplaced(state, world, pos, moved)
    }
}
