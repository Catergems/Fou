package com.fou.blockentity

import com.fou.power.PowerConstants
import com.fou.registry.ModBlockEntities
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class CoolantBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.COOLANT_BLOCK, pos, state) {

    val inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(1, ItemStack.EMPTY)

    var heat: Float = 0f
        private set

    val heatPercent: Float get() = heat / PowerConstants.HEAT_MAX.toFloat()

    fun addHeat(amount: Float) { heat = (heat + amount).coerceAtMost(PowerConstants.HEAT_MAX.toFloat()) }
    fun coolDown(amount: Float) { heat = (heat - amount).coerceAtLeast(0f) }
    fun isExploding(): Boolean = heat >= PowerConstants.HEAT_EXPLODE

    // ── Coolant fuel helpers ─────────────────────────────────────────────────
    val hasFuel: Boolean get() = !inventory[0].isEmpty

    // Cooling power per tick based on fuel type
    val coolingPower: Float get() = when {
        inventory[0].isOf(Items.PACKED_ICE)    -> 8.0f
        inventory[0].isOf(Items.ICE)           -> 4.0f
        inventory[0].isOf(Items.WATER_BUCKET)  -> 2.0f
        else -> 0f
    }

    // Durability of fuel (how many ticks it lasts)
    // We'll use a separate counter
    var fuelTicksLeft: Int = 0

    override fun readData(view: ReadView) {
        super.readData(view)
        heat         = view.getFloat("Heat", 0f)
        fuelTicksLeft = view.getInt("FuelTicks", 0)
        view.read("FuelItem", ItemStack.CODEC).ifPresent { inventory[0] = it }
    }

    override fun writeData(view: WriteView) {
        super.writeData(view)
        view.putFloat("Heat", heat)
        view.putInt("FuelTicks", fuelTicksLeft)
        if (!inventory[0].isEmpty) view.put("FuelItem", ItemStack.CODEC, inventory[0])
    }

    companion object {
        // Fuel duration in ticks
        private const val WATER_BUCKET_TICKS = 600  // 30s
        private const val ICE_TICKS          = 1200 // 60s
        private const val PACKED_ICE_TICKS   = 2400 // 120s

        fun tick(world: World, pos: BlockPos, state: BlockState, be: CoolantBlockEntity) {
            if (world.isClient) return

            // Consume fuel tick
            if (be.hasFuel && be.fuelTicksLeft > 0) {
                be.fuelTicksLeft--
                if (be.fuelTicksLeft <= 0) {
                    // Return empty bucket if water, else just consume
                    if (be.inventory[0].isOf(Items.WATER_BUCKET)) {
                        be.inventory[0] = ItemStack(Items.BUCKET)
                    } else {
                        be.inventory[0] = ItemStack.EMPTY
                    }
                }
            }

            val cooling = be.coolingPower

            // Absorb heat from all 6 adjacent machines
            Direction.entries.forEach { dir ->
                val neighborPos = pos.offset(dir)
                val neighborBe  = world.getBlockEntity(neighborPos) ?: return@forEach

                val neighborHeat = when (neighborBe) {
                    is PowerGeneratorBlockEntity    -> neighborBe
                    is VoltageStabilizerBlockEntity -> neighborBe
                    is CrusherBlockEntity           -> neighborBe
                    else -> null
                }

                if (neighborHeat != null) {
                    val absorbed = cooling.coerceAtMost(
                        when (neighborBe) {
                            is PowerGeneratorBlockEntity    -> neighborBe.heat
                            is VoltageStabilizerBlockEntity -> neighborBe.heat
                            else -> 0f
                        }
                    )

                    // Cool the neighbor
                    when (neighborBe) {
                        is PowerGeneratorBlockEntity    -> neighborBe.coolDown(cooling)
                        is VoltageStabilizerBlockEntity -> neighborBe.coolDown(cooling)
                    }

                    // If no fuel, or neighbor heat is too high, coolant itself heats up
                    if (cooling == 0f) {
                        be.addHeat(PowerConstants.heatGainPerTick(be.heat.toInt(), absorbed + 10f))
                    }
                }
            }

            // Coolant self-cools when it has fuel and isn't absorbing too much
            if (cooling > 0f) {
                be.coolDown(cooling * 0.5f)
            }

            if (be.isExploding()) {
                world.removeBlock(pos, false)
                world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 2.5f, World.ExplosionSourceType.TNT)
                return
            }

            be.markDirty()
            world.updateListeners(pos, state, state, net.minecraft.block.Block.NOTIFY_ALL)
        }

        fun getFuelTicks(stack: ItemStack): Int = when {
            stack.isOf(Items.WATER_BUCKET) -> WATER_BUCKET_TICKS
            stack.isOf(Items.ICE)          -> ICE_TICKS
            stack.isOf(Items.PACKED_ICE)   -> PACKED_ICE_TICKS
            else -> 0
        }
    }
}
