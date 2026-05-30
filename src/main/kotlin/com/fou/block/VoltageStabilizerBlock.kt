package com.fou.block

import com.fou.blockentity.VoltageStabilizerBlockEntity
import com.fou.registry.ModBlockEntities
import com.mojang.serialization.MapCodec
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class VoltageStabilizerBlock(settings: Settings) : BlockWithEntity(settings) {

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

    override fun getCodec(): MapCodec<out BlockWithEntity> = createCodec(::VoltageStabilizerBlock)
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        VoltageStabilizerBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (world.isClient) return null
        return validateTicker(type, ModBlockEntities.VOLTAGE_STABILIZER, VoltageStabilizerBlockEntity::tick)
    }

    override fun onUse(
        state: BlockState, world: World, pos: BlockPos,
        player: PlayerEntity, hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.PASS
        val be = world.getBlockEntity(pos) as? VoltageStabilizerBlockEntity ?: return ActionResult.PASS

        // TODO: open owo-lib GUI screen when implemented
        val status   = if (be.isActive) "§aActive §f${be.outputWatts.toInt()}W" else "§cStandby §7(input too low)"
        val heatPct  = "%.1f".format(be.heatPercent * 100f)
        val target   = be.targetVoltage.toInt()
        val input    = be.inputWatts.toInt()
        player.sendMessage(
            Text.literal("§6[Stabilizer] §f$status §7| Target: §e${target}W §7| Input: §b${input}W §7| Heat: §c$heatPct%"), true
        )
        return ActionResult.SUCCESS
    }

    override fun onStateReplaced(
        state: BlockState, world: ServerWorld, pos: BlockPos, moved: Boolean
    ) {
        super.onStateReplaced(state, world, pos, moved)
    }
}
