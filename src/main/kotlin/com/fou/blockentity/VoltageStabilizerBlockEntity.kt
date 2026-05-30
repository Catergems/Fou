package com.fou.blockentity

import com.fou.power.PowerConstants
import com.fou.registry.ModBlockEntities
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class VoltageStabilizerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.VOLTAGE_STABILIZER, pos, state) {

    // Set voltage (0-240W), configured via owo-lib GUI slider
    var targetVoltage: Float = 0f

    // Input power from linked generator this tick
    var inputWatts: Float = 0f

    // Output power (equals targetVoltage if input >= target, else 0)
    val outputWatts: Float
        get() = if (inputWatts >= targetVoltage) targetVoltage else 0f

    val isActive: Boolean
        get() = outputWatts > 0f

    // Heat (0-1000): builds when input >> target (excess energy)
    var heat: Float = 0f
        private set

    val heatPercent: Float get() = heat / PowerConstants.HEAT_MAX.toFloat()

    fun addHeat(amount: Float) {
        heat = (heat + amount).coerceAtMost(PowerConstants.HEAT_MAX.toFloat())
    }

    fun coolDown(amount: Float) {
        heat = (heat - amount).coerceAtLeast(0f)
    }

    fun isExploding(): Boolean = heat >= PowerConstants.HEAT_EXPLODE

    // ── Serialization ────────────────────────────────────────────────────────
    override fun readData(view: ReadView) {
        super.readData(view)
        targetVoltage = view.getFloat("TargetVoltage", 0f)
        heat          = view.getFloat("Heat", 0f)
    }

    override fun writeData(view: WriteView) {
        super.writeData(view)
        view.putFloat("TargetVoltage", targetVoltage)
        view.putFloat("Heat", heat)
    }

    // ── Server tick ──────────────────────────────────────────────────────────
    companion object {
        fun tick(world: World, pos: BlockPos, state: BlockState, be: VoltageStabilizerBlockEntity) {
            if (world.isClient) return

            // Heat builds when input is higher than target (excess being throttled)
            val excess = (be.inputWatts - be.targetVoltage).coerceAtLeast(0f)
            if (excess > 0f) {
                be.addHeat(PowerConstants.heatGainPerTick(be.heat.toInt(), excess))
            } else {
                be.coolDown(PowerConstants.HEAT_COOL_PER_TICK)
            }

            // Reset input for next tick
            be.inputWatts = 0f

            if (be.isExploding()) {
                explode(world, pos)
                return
            }

            be.markDirty()
            world.updateListeners(pos, state, state, net.minecraft.block.Block.NOTIFY_ALL)
        }

        private fun explode(world: World, pos: BlockPos) {
            world.removeBlock(pos, false)
            world.createExplosion(
                null,
                pos.x.toDouble() + 0.5,
                pos.y.toDouble() + 0.5,
                pos.z.toDouble() + 0.5,
                3.0f,
                World.ExplosionSourceType.TNT
            )
        }
    }
}
