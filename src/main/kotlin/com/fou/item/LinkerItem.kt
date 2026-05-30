package com.fou.item

import com.fou.blockentity.PowerGeneratorBlockEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class LinkerItem(settings: Settings) : Item(settings) {

    companion object {
        const val MAX_RANGE = 16
        private const val KEY_SOURCE_X = "SourceX"
        private const val KEY_SOURCE_Y = "SourceY"
        private const val KEY_SOURCE_Z = "SourceZ"
        private const val KEY_HAS_SOURCE = "HasSource"
    }

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world  = context.world
        val pos    = context.blockPos
        val player = context.player ?: return ActionResult.PASS
        val stack  = context.stack

        if (world.isClient) return ActionResult.SUCCESS

        val be = world.getBlockEntity(pos)

        // ── First click: select source (must be PowerGenerator) ──────────────
        if (!hasSource(stack)) {
            if (be !is PowerGeneratorBlockEntity) {
                player.sendMessage(Text.literal("§cFirst click must be on a Power Generator!"), true)
                return ActionResult.FAIL
            }
            setSource(stack, pos)
            player.sendMessage(Text.literal("§aSource selected! §7Now click the target machine."), true)
            return ActionResult.SUCCESS
        }

        // ── Second click: link source to target ──────────────────────────────
        val sourcePos = getSource(stack)!!

        if (sourcePos == pos) {
            player.sendMessage(Text.literal("§cCan't link a block to itself!"), true)
            return ActionResult.FAIL
        }

        if (!isInRange(sourcePos, pos)) {
            player.sendMessage(Text.literal("§cOut of range! Max ${MAX_RANGE} blocks."), true)
            clearSource(stack)
            return ActionResult.FAIL
        }

        val sourceEntity = world.getBlockEntity(sourcePos)
        if (sourceEntity !is PowerGeneratorBlockEntity) {
            player.sendMessage(Text.literal("§cSource block is no longer valid!"), true)
            clearSource(stack)
            return ActionResult.FAIL
        }

        // Target must have a block entity (any machine)
        if (world.getBlockEntity(pos) == null) {
            player.sendMessage(Text.literal("§cTarget must be a machine block!"), true)
            return ActionResult.FAIL
        }

        sourceEntity.addLinkedPos(pos)
        sourceEntity.markDirty()
        clearSource(stack)

        player.sendMessage(
            Text.literal("§aLinked! §7Generator §f$sourcePos §7→ §fblock $pos"), true
        )
        return ActionResult.SUCCESS
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        val stack = user.getStackInHand(hand)
        if (!world.isClient && user.isSneaking && hasSource(stack)) {
            clearSource(stack)
            user.sendMessage(Text.literal("§eLinker reset."), true)
        }
        return ActionResult.PASS
    }

    // ── Data component helpers ────────────────────────────────────────────────
    private fun getNbt(stack: ItemStack): NbtCompound? =
        stack.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt()

    private fun hasSource(stack: ItemStack): Boolean =
        getNbt(stack)?.getBoolean(KEY_HAS_SOURCE, false) ?: false

    private fun setSource(stack: ItemStack, pos: BlockPos) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack) { nbt ->
            nbt.putBoolean(KEY_HAS_SOURCE, true)
            nbt.putInt(KEY_SOURCE_X, pos.x)
            nbt.putInt(KEY_SOURCE_Y, pos.y)
            nbt.putInt(KEY_SOURCE_Z, pos.z)
        }
    }

    private fun getSource(stack: ItemStack): BlockPos? {
        val nbt = getNbt(stack) ?: return null
        if (!nbt.getBoolean(KEY_HAS_SOURCE, false)) return null
        return BlockPos(
            nbt.getInt(KEY_SOURCE_X, 0),
            nbt.getInt(KEY_SOURCE_Y, 0),
            nbt.getInt(KEY_SOURCE_Z, 0)
        )
    }

    private fun clearSource(stack: ItemStack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack) { nbt ->
            nbt.putBoolean(KEY_HAS_SOURCE, false)
        }
    }

    private fun isInRange(a: BlockPos, b: BlockPos): Boolean =
        a.getSquaredDistance(b) <= (MAX_RANGE * MAX_RANGE).toDouble()
}
