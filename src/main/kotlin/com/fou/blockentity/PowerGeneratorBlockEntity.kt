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
import net.minecraft.world.World
import kotlin.random.Random

class PowerGeneratorBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.POWER_GENERATOR, pos, state) {

    // ── Inventory: 1 fuel slot ───────────────────────────────────────────────
    val inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(1, ItemStack.EMPTY)

    // ── Power state ──────────────────────────────────────────────────────────
    var currentWatts: Float = 0f       // current output W
    var burnTicksLeft: Int  = 0        // ticks remaining on current fuel
    var isRunning: Boolean  = false

    // ── Heat state (0-1000) ──────────────────────────────────────────────────
    var heat: Float = 0f
        private set

    val heatPercent: Float get() = heat / PowerConstants.HEAT_MAX.toFloat()

    // ── Power consumption tracking ───────────────────────────────────────────
    // Set by the power distribution system each tick
    var powerConsumed: Float = 0f

    // ── Serialization ────────────────────────────────────────────────────────
    override fun readData(view: ReadView) {
        super.readData(view)
        currentWatts  = view.getFloat("Watts", 0f)
        burnTicksLeft = view.getInt("BurnTicks", 0)
        heat          = view.getFloat("Heat", 0f)
        isRunning     = view.getBoolean("Running", false)
        view.read("FuelItem", ItemStack.CODEC).ifPresent { inventory[0] = it }
    }

    override fun writeData(view: WriteView) {
        super.writeData(view)
        view.putFloat("Watts", currentWatts)
        view.putInt("BurnTicks", burnTicksLeft)
        view.putFloat("Heat", heat)
        view.putBoolean("Running", isRunning)
        if (!inventory[0].isEmpty) view.put("FuelItem", ItemStack.CODEC, inventory[0])
    }

    // ── Heat helpers ─────────────────────────────────────────────────────────
    fun addHeat(amount: Float) {
        heat = (heat + amount).coerceAtMost(PowerConstants.HEAT_MAX.toFloat())
    }

    fun coolDown(amount: Float) {
        heat = (heat - amount).coerceAtLeast(0f)
    }

    fun isExploding(): Boolean = heat >= PowerConstants.HEAT_EXPLODE

    // ── Fuel helpers ─────────────────────────────────────────────────────────
    fun tryConsumeFuel(): Boolean {
        val fuel = inventory[0]
        if (fuel.isEmpty) return false

        val (watts, ticks) = getFuelStats(fuel) ?: return false
        currentWatts  = watts
        burnTicksLeft = ticks
        isRunning     = true
        fuel.decrement(1)
        markDirty()
        return true
    }

    private fun getFuelStats(stack: ItemStack): Pair<Float, Int>? {
        val ticks = Random.nextInt(PowerConstants.BURN_TICKS_MIN, PowerConstants.BURN_TICKS_MAX + 1)
        return when {
            stack.isOf(Items.COAL) || stack.isOf(Items.CHARCOAL) ->
                Pair(Random.nextFloat() * (PowerConstants.COAL_W_MAX - PowerConstants.COAL_W_MIN) + PowerConstants.COAL_W_MIN, ticks)
            stack.isOf(Items.COAL_BLOCK) ->
                Pair(PowerConstants.COAL_BLOCK_W_MAX, ticks)
            stack.isOf(Items.GLOWSTONE_DUST) ->
                Pair(PowerConstants.GLOW_DUST_W_MAX, ticks)
            stack.isOf(Items.GLOWSTONE) ->
                Pair(PowerConstants.GLOW_BLOCK_W_MAX, ticks)
            else -> null
        }
    }

    // ── Server tick ──────────────────────────────────────────────────────────
    companion object {
        fun tick(world: World, pos: BlockPos, state: BlockState, be: PowerGeneratorBlockEntity) {
            if (world.isClient) return

            // Try to start burning if idle
            if (!be.isRunning || be.burnTicksLeft <= 0) {
                be.isRunning = false
                be.currentWatts = 0f
                if (!be.tryConsumeFuel()) {
                    // No fuel — cool down passively
                    be.coolDown(PowerConstants.HEAT_COOL_PER_TICK)
                    be.markDirty()
                    return
                }
            }

            // Burn tick
            be.burnTicksLeft--

            // Heat logic — if power has nowhere to go, heat builds
            val wastedWatts = (be.currentWatts - be.powerConsumed).coerceAtLeast(0f)
            if (wastedWatts > 0f) {
                be.addHeat(PowerConstants.heatGainPerTick(be.heat.toInt(), wastedWatts))
            } else {
                be.coolDown(PowerConstants.HEAT_COOL_PER_TICK)
            }

            // Reset consumed power for next tick
            be.powerConsumed = 0f

            // Explosion check
            if (be.isExploding()) {
                explode(world, pos, be)
                return
            }

            // Smoke/fire particles at high heat (handled client-side via block state later)
            be.markDirty()
            world.updateListeners(pos, state, state, net.minecraft.block.Block.NOTIFY_ALL)
        }

        private fun explode(world: World, pos: BlockPos, be: PowerGeneratorBlockEntity) {
            world.removeBlock(pos, false)
            world.createExplosion(
                null,
                pos.x.toDouble() + 0.5,
                pos.y.toDouble() + 0.5,
                pos.z.toDouble() + 0.5,
                3.5f,
                World.ExplosionSourceType.TNT
            )
        }
    }
}
