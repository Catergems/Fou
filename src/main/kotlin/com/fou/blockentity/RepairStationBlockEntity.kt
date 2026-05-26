package com.fou.blockentity

import com.fou.registry.ModBlockEntities
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class RepairStationBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.REPAIR_STATION, pos, state) {

    companion object {
        const val MAX_CHARGES             = 1000   // 500 display charges
        const val GLOWSTONE_DUST_CHARGES  = 2      // 1 display charge
        const val GLOWSTONE_BLOCK_CHARGES = 30     // 15 display charges
        private const val REPAIR_INTERVAL = 5      // ticks (4x/sec)

        fun tick(world: World, pos: BlockPos, state: BlockState, be: RepairStationBlockEntity) {
            if (world.isClient) return
            be.tickCounter++
            if (be.tickCounter < REPAIR_INTERVAL) return
            be.tickCounter = 0

            val stack = be.inventory[0]
            if (stack.isEmpty || !stack.isDamageable || stack.damage <= 0 || be.charges <= 0) return

            stack.damage = (stack.damage - 1).coerceAtLeast(0)
            be.charges   = (be.charges - 1).coerceAtLeast(0)

            if (stack.damage == 0) {
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.BLOCKS, 1.0f, 1.5f)
            }
            be.markDirty()
            world.updateListeners(pos, state, state, net.minecraft.block.Block.NOTIFY_ALL)
        }
    }

    val inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(1, ItemStack.EMPTY)
    var charges: Int = 0
    private var tickCounter: Int = 0

    val chargesDisplay: String
        get() {
            val whole = charges / 2
            val half  = charges % 2 != 0
            return if (half) "$whole.5" else "$whole"
        }

    val chargesDisplayMax: Int get() = MAX_CHARGES / 2  // 500

    fun addCharges(units: Int) { charges = (charges + units).coerceAtMost(MAX_CHARGES) }
    fun isFull(): Boolean = charges >= MAX_CHARGES

    // ── Serialization (1.21.11 ReadView/WriteView API) ─────────────────────
    override fun readData(view: ReadView) {
        super.readData(view)
        charges = view.getInt("Charges", 0)
        view.read("Item0", ItemStack.CODEC).ifPresent { inventory[0] = it }
    }

    override fun writeData(view: WriteView) {
        super.writeData(view)
        view.putInt("Charges", charges)
        if (!inventory[0].isEmpty) view.put("Item0", ItemStack.CODEC, inventory[0])
    }
}
