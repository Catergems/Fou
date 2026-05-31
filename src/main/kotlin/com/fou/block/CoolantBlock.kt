package com.fou.block

import com.fou.blockentity.CoolantBlockEntity
import com.fou.registry.ModBlockEntities
import com.mojang.serialization.MapCodec
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class CoolantBlock(settings: Settings) : BlockWithEntity(settings) {

    override fun getCodec(): MapCodec<out BlockWithEntity> = createCodec(::CoolantBlock)
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CoolantBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (world.isClient) return null
        return validateTicker(type, ModBlockEntities.COOLANT_BLOCK, CoolantBlockEntity::tick)
    }

    override fun onUse(
        state: BlockState, world: World, pos: BlockPos,
        player: PlayerEntity, hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.PASS
        val be = world.getBlockEntity(pos) as? CoolantBlockEntity ?: return ActionResult.PASS
        val handStack = player.getStackInHand(net.minecraft.util.Hand.MAIN_HAND)

        // Sneak + RMB → retrieve fuel
        if (player.isSneaking) {
            val fuel = be.inventory[0]
            if (!fuel.isEmpty) {
                if (!player.inventory.insertStack(fuel)) player.dropItem(fuel, false)
                be.inventory[0] = ItemStack.EMPTY
                be.fuelTicksLeft = 0
                be.markDirty()
                player.sendMessage(Text.literal("§eCoolant retrieved."), true)
            } else {
                player.sendMessage(Text.literal("§7No coolant inserted."), true)
            }
            return ActionResult.SUCCESS
        }

        // RMB with coolant fuel → insert
        if (!handStack.isEmpty && isCoolant(handStack)) {
            if (!be.inventory[0].isEmpty) {
                player.sendMessage(Text.literal("§cCoolant slot already occupied!"), true)
                return ActionResult.FAIL
            }
            val ticks = CoolantBlockEntity.getFuelTicks(handStack)
            be.inventory[0] = handStack.copyWithCount(1)
            be.fuelTicksLeft = ticks
            handStack.decrement(1)
            // Give back empty bucket if water bucket
            if (be.inventory[0].isOf(Items.WATER_BUCKET)) {
                // keep as water bucket in slot, return empty bucket when used up
            }
            be.markDirty()
            val duration = ticks / 20
            player.sendMessage(Text.literal("§aCoolant inserted! §7Duration: §f${duration}s"), true)
            return ActionResult.SUCCESS
        }

        // Show status
        val heatPct  = "%.1f".format(be.heatPercent * 100f)
        val fuelName = if (be.inventory[0].isEmpty) "§7(empty)" else be.inventory[0].name.string
        val ticksLeft = be.fuelTicksLeft / 20
        player.sendMessage(
            Text.literal("§b[Coolant] §fHeat: §c$heatPct% §7| Fuel: §f$fuelName §7| Remaining: §f${ticksLeft}s"), true
        )
        return ActionResult.SUCCESS
    }

    private fun isCoolant(stack: ItemStack): Boolean =
        stack.isOf(Items.WATER_BUCKET) || stack.isOf(Items.ICE) || stack.isOf(Items.PACKED_ICE)

    override fun onStateReplaced(state: BlockState, world: ServerWorld, pos: BlockPos, moved: Boolean) {
        if (!moved) {
            val be = world.getBlockEntity(pos) as? CoolantBlockEntity
            be?.inventory?.forEach { stack ->
                if (!stack.isEmpty) ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
            }
        }
        super.onStateReplaced(state, world, pos, moved)
    }
}
