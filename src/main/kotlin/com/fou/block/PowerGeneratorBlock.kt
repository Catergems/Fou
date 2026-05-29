package com.fou.block

import com.fou.blockentity.PowerGeneratorBlockEntity
import com.fou.registry.ModBlockEntities
import com.mojang.serialization.MapCodec
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class PowerGeneratorBlock(settings: Settings) : BlockWithEntity(settings) {

    override fun getCodec(): MapCodec<out BlockWithEntity> = createCodec(::PowerGeneratorBlock)
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PowerGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (world.isClient) return null
        return validateTicker(type, ModBlockEntities.POWER_GENERATOR, PowerGeneratorBlockEntity::tick)
    }

    override fun onUse(
        state: BlockState, world: World, pos: BlockPos,
        player: PlayerEntity, hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.PASS
        val be = world.getBlockEntity(pos) as? PowerGeneratorBlockEntity ?: return ActionResult.PASS
        val hand = net.minecraft.util.Hand.MAIN_HAND
        val handStack = player.getStackInHand(hand)

        // Sneak + RMB → retrieve fuel
        if (player.isSneaking) {
            val fuel = be.inventory[0]
            if (!fuel.isEmpty) {
                if (!player.inventory.insertStack(fuel)) player.dropItem(fuel, false)
                be.inventory[0] = ItemStack.EMPTY
                be.markDirty()
                player.sendMessage(Text.literal("§eFuel retrieved."), true)
            } else {
                player.sendMessage(Text.literal("§7No fuel inserted."), true)
            }
            return ActionResult.SUCCESS
        }

        // RMB with fuel → insert
        if (!handStack.isEmpty && isFuel(handStack)) {
            if (!be.inventory[0].isEmpty) {
                player.sendMessage(Text.literal("§cFuel slot already occupied!"), true)
                return ActionResult.FAIL
            }
            be.inventory[0] = handStack.copyWithCount(1)
            handStack.decrement(1)
            be.markDirty()
            player.sendMessage(Text.literal("§aFuel inserted!"), true)
            return ActionResult.SUCCESS
        }

        // RMB with nothing → show status
        val heatPct  = "%.1f".format(be.heatPercent * 100f)
        val fuelName = if (be.inventory[0].isEmpty) "§7none" else be.inventory[0].name.string
        val status   = if (be.isRunning) "§aRunning §f${be.currentWatts.toInt()}W" else "§cIdle"
        player.sendMessage(
            Text.literal("§6[Generator] §f$status §7| Heat: §c$heatPct% §7| Fuel: §f$fuelName"), true
        )
        return ActionResult.SUCCESS
    }

    private fun isFuel(stack: ItemStack): Boolean = stack.isOf(net.minecraft.item.Items.COAL)
            || stack.isOf(net.minecraft.item.Items.CHARCOAL)
            || stack.isOf(net.minecraft.item.Items.COAL_BLOCK)
            || stack.isOf(net.minecraft.item.Items.GLOWSTONE_DUST)
            || stack.isOf(net.minecraft.item.Items.GLOWSTONE)

    override fun onStateReplaced(
        state: BlockState, world: ServerWorld, pos: BlockPos, moved: Boolean
    ) {
        if (!moved) {
            val be = world.getBlockEntity(pos) as? PowerGeneratorBlockEntity
            be?.inventory?.forEach { stack ->
                if (!stack.isEmpty) ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
            }
        }
        super.onStateReplaced(state, world, pos, moved)
    }
}
