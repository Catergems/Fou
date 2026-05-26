package com.fou.block

import com.fou.blockentity.RepairStationBlockEntity
import com.fou.registry.ModBlockEntities
import com.mojang.serialization.MapCodec
import net.fabricmc.fabric.api.event.player.UseBlockCallback
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
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class RepairStationBlock(settings: Settings) : BlockWithEntity(settings) {

    companion object {
        fun registerUseCallback() {
            UseBlockCallback.EVENT.register(UseBlockCallback { player, world, hand, hit ->
                if (!player.isSneaking) return@UseBlockCallback ActionResult.PASS

                val state = world.getBlockState(hit.blockPos)
                val block = state.block as? RepairStationBlock
                    ?: return@UseBlockCallback ActionResult.PASS

                block.handleUse(state, world, hit.blockPos, player, player.getStackInHand(hand))
            })
        }
    }

    override fun getCodec(): MapCodec<out BlockWithEntity> = createCodec(::RepairStationBlock)

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        RepairStationBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (world.isClient) return null
        return validateTicker(type, ModBlockEntities.REPAIR_STATION, RepairStationBlockEntity::tick)
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hit: BlockHitResult
    ): ActionResult {
        val hand = Hand.MAIN_HAND
        return handleUse(state, world, pos, player, player.getStackInHand(hand))
    }

    override fun onUseWithItem(
        stack: ItemStack,
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult = handleUse(state, world, pos, player, stack)

    private fun handleUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        handStack: ItemStack
    ): ActionResult {
        // Accept on the client so held items like glowstone do not run vanilla placement/use first.
        if (world.isClient) return ActionResult.SUCCESS

        val be = world.getBlockEntity(pos) as? RepairStationBlockEntity
            ?: return ActionResult.PASS

        val maxDisplay = be.chargesDisplayMax

        // ── Sneak + RMB → Charge ──────────────────────────────────────────
        if (player.isSneaking) {
            when {
                handStack.isOf(Items.GLOWSTONE) -> {
                    if (be.isFull()) {
                        player.sendMessage(Text.literal("§cFully charged! §7(${be.chargesDisplay}/$maxDisplay)"), true)
                        return ActionResult.SUCCESS
                    }
                    be.addCharges(RepairStationBlockEntity.GLOWSTONE_BLOCK_CHARGES)
                    if (!player.isCreative) handStack.decrement(1)
                    player.sendMessage(Text.literal("§aCharged +15! §f${be.chargesDisplay}/$maxDisplay charges"), true)
                    be.markDirty()
                    world.updateListeners(pos, state, state, Block.NOTIFY_ALL)
                    return ActionResult.SUCCESS
                }
                handStack.isOf(Items.GLOWSTONE_DUST) -> {
                    if (be.isFull()) {
                        player.sendMessage(Text.literal("§cFully charged! §7(${be.chargesDisplay}/$maxDisplay)"), true)
                        return ActionResult.SUCCESS
                    }
                    be.addCharges(RepairStationBlockEntity.GLOWSTONE_DUST_CHARGES)
                    if (!player.isCreative) handStack.decrement(1)
                    player.sendMessage(Text.literal("§aCharged +1! §f${be.chargesDisplay}/$maxDisplay charges"), true)
                    be.markDirty()
                    world.updateListeners(pos, state, state, Block.NOTIFY_ALL)
                    return ActionResult.SUCCESS
                }
                else -> {
                    // Show status when sneaking with anything else
                    val itemName = if (be.inventory[0].isEmpty) "none" else be.inventory[0].name.string
                    player.sendMessage(Text.literal("§6[Repair Station] §fCharges: ${be.chargesDisplay}/$maxDisplay §7| Item: §f$itemName"), true)
                    return ActionResult.SUCCESS
                }
            }
        }

        // ── Normal RMB ────────────────────────────────────────────────────
        val slotStack = be.inventory[0]

        // Slot has item → give it back to player (no copy = no dupe)
        if (!slotStack.isEmpty) {
            if (!player.inventory.insertStack(slotStack)) {
                // Inventory full: drop at player's feet
                player.dropItem(slotStack, false)
            }
            be.inventory[0] = ItemStack.EMPTY
            be.markDirty()
            world.updateListeners(pos, state, state, Block.NOTIFY_ALL)
            player.sendMessage(Text.literal("§eItem retrieved."), true)
            return ActionResult.SUCCESS
        }

        // Slot empty + player holding a damaged item → insert it
        if (!handStack.isEmpty && handStack.isDamageable && handStack.damage > 0) {
            // Move the actual stack into the BE (no copy = no dupe)
            be.inventory[0] = handStack.copyWithCount(1)
            handStack.decrement(1)
            be.markDirty()
            world.updateListeners(pos, state, state, Block.NOTIFY_ALL)
            player.sendMessage(Text.literal("§aItem inserted! §fCharges: ${be.chargesDisplay}/$maxDisplay"), true)
            return ActionResult.SUCCESS
        }

        // Otherwise just show status
        player.sendMessage(Text.literal("§6[Repair Station] §fCharges: ${be.chargesDisplay}/$maxDisplay §7| Slot: §fempty"), true)
        return ActionResult.SUCCESS
    }

    override fun onStateReplaced(
        state: BlockState,
        world: ServerWorld,
        pos: BlockPos,
        moved: Boolean
    ) {
        if (!moved) {
            val be = world.getBlockEntity(pos) as? RepairStationBlockEntity
            be?.inventory?.forEach { stack ->
                if (!stack.isEmpty) {
                    ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                }
            }
        }
        super.onStateReplaced(state, world, pos, moved)
    }
}
