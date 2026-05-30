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

    var targetVoltage: Float = 0f
    var inputWatts: Float = 0f

    val outputWatts: Float
        get() = if (inputWatts >= targetVoltage && targetVoltage > 0f) targetVoltage else 0f

    val isActive: Boolean get() = outputWatts > 0f

    var heat: Float = 0f
        private set

    val heatPercent: Float get() = heat / PowerConstants.HEAT_MAX.toFloat()

    // ── Linked downstream machines ───────────────────────────────────────────
    val linkedPositions: MutableList<BlockPos> = mutableListOf()

    fun addLinkedPos(pos: BlockPos) { if (!linkedPositions.contains(pos)) linkedPositions.add(pos) }
    fun removeLinkedPos(pos: BlockPos) { linkedPositions.remove(pos) }

    fun addHeat(amount: Float) { heat = (heat + amount).coerceAtMost(PowerConstants.HEAT_MAX.toFloat()) }
    fun coolDown(amount: Float) { heat = (heat - amount).coerceAtLeast(0f) }
    fun isExploding(): Boolean = heat >= PowerConstants.HEAT_EXPLODE

    override fun readData(view: ReadView) {
        super.readData(view)
        targetVoltage = view.getFloat("TargetVoltage", 0f)
        heat          = view.getFloat("Heat", 0f)
        linkedPositions.clear()
        view.getOptionalTypedListView("LinkedPositions", BlockPos.CODEC)
            .ifPresent { it.forEach { pos -> linkedPositions.add(pos) } }
    }

    override fun writeData(view: WriteView) {
        super.writeData(view)
        view.putFloat("TargetVoltage", targetVoltage)
        view.putFloat("Heat", heat)
        val appender = view.getListAppender("LinkedPositions", BlockPos.CODEC)
        linkedPositions.forEach { appender.add(it) }
    }

    companion object {
        fun tick(world: World, pos: BlockPos, state: BlockState, be: VoltageStabilizerBlockEntity) {
            if (world.isClient) return

            val excess = (be.inputWatts - be.targetVoltage).coerceAtLeast(0f)
            if (excess > 0f) {
                be.addHeat(PowerConstants.heatGainPerTick(be.heat.toInt(), excess))
            } else {
                be.coolDown(PowerConstants.HEAT_COOL_PER_TICK)
            }

            // Forward output power to linked machines
            if (be.linkedPositions.isNotEmpty() && be.outputWatts > 0f) {
                val wattsPerMachine = be.outputWatts / be.linkedPositions.size
                val deadLinks = mutableListOf<BlockPos>()
                be.linkedPositions.forEach { linkedPos ->
                    when (val linkedBe = world.getBlockEntity(linkedPos)) {
                        is CrusherBlockEntity -> linkedBe.inputWatts += wattsPerMachine
                        null -> deadLinks.add(linkedPos)
                    }
                }
                deadLinks.forEach { be.removeLinkedPos(it) }
            }

            be.inputWatts = 0f

            if (be.isExploding()) {
                world.removeBlock(pos, false)
                world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 3.0f, World.ExplosionSourceType.TNT)
                return
            }

            be.markDirty()
            world.updateListeners(pos, state, state, net.minecraft.block.Block.NOTIFY_ALL)
        }
    }
}
