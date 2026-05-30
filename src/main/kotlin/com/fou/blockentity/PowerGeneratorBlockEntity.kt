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

    val inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(1, ItemStack.EMPTY)

    var currentWatts: Float = 0f
    var burnTicksLeft: Int  = 0
    var isRunning: Boolean  = false

    var heat: Float = 0f
        private set

    val heatPercent: Float get() = heat / PowerConstants.HEAT_MAX.toFloat()
    var powerConsumed: Float = 0f

    val linkedPositions: MutableList<BlockPos> = mutableListOf()

    fun addLinkedPos(pos: BlockPos) { if (!linkedPositions.contains(pos)) linkedPositions.add(pos) }
    fun removeLinkedPos(pos: BlockPos) { linkedPositions.remove(pos) }

    override fun readData(view: ReadView) {
        super.readData(view)
        currentWatts  = view.getFloat("Watts", 0f)
        burnTicksLeft = view.getInt("BurnTicks", 0)
        heat          = view.getFloat("Heat", 0f)
        isRunning     = view.getBoolean("Running", false)
        view.read("FuelItem", ItemStack.CODEC).ifPresent { inventory[0] = it }
        linkedPositions.clear()
        view.getOptionalTypedListView("LinkedPositions", BlockPos.CODEC)
            .ifPresent { listView -> listView.forEach { linkedPositions.add(it) } }
    }

    override fun writeData(view: WriteView) {
        super.writeData(view)
        view.putFloat("Watts", currentWatts)
        view.putInt("BurnTicks", burnTicksLeft)
        view.putFloat("Heat", heat)
        view.putBoolean("Running", isRunning)
        if (!inventory[0].isEmpty) view.put("FuelItem", ItemStack.CODEC, inventory[0])
        val appender = view.getListAppender("LinkedPositions", BlockPos.CODEC)
        linkedPositions.forEach { appender.add(it) }
    }

    fun addHeat(amount: Float) { heat = (heat + amount).coerceAtMost(PowerConstants.HEAT_MAX.toFloat()) }
    fun coolDown(amount: Float) { heat = (heat - amount).coerceAtLeast(0f) }
    fun isExploding(): Boolean = heat >= PowerConstants.HEAT_EXPLODE

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
            stack.isOf(Items.COAL_BLOCK) -> Pair(PowerConstants.COAL_BLOCK_W_MAX, ticks)
            stack.isOf(Items.GLOWSTONE_DUST) -> Pair(PowerConstants.GLOW_DUST_W_MAX, ticks)
            stack.isOf(Items.GLOWSTONE) -> Pair(PowerConstants.GLOW_BLOCK_W_MAX, ticks)
            else -> null
        }
    }

    companion object {
        fun tick(world: World, pos: BlockPos, state: BlockState, be: PowerGeneratorBlockEntity) {
            if (world.isClient) return

            if (!be.isRunning || be.burnTicksLeft <= 0) {
                be.isRunning = false
                be.currentWatts = 0f
                if (!be.tryConsumeFuel()) {
                    be.coolDown(PowerConstants.HEAT_COOL_PER_TICK)
                    be.markDirty()
                    return
                }
            }

            be.burnTicksLeft--

            if (be.linkedPositions.isNotEmpty() && be.isRunning) {
                val wattsPerMachine = be.currentWatts / be.linkedPositions.size
                val deadLinks = mutableListOf<BlockPos>()
                be.linkedPositions.forEach { linkedPos ->
                    val linkedBe = world.getBlockEntity(linkedPos)
                    if (linkedBe is VoltageStabilizerBlockEntity) {
                        linkedBe.inputWatts += wattsPerMachine
                        be.powerConsumed += wattsPerMachine
                    } else {
                        deadLinks.add(linkedPos)
                    }
                }
                deadLinks.forEach { be.removeLinkedPos(it) }
            }

            val wastedWatts = (be.currentWatts - be.powerConsumed).coerceAtLeast(0f)
            if (wastedWatts > 0f) {
                be.addHeat(PowerConstants.heatGainPerTick(be.heat.toInt(), wastedWatts))
            } else {
                be.coolDown(PowerConstants.HEAT_COOL_PER_TICK)
            }

            be.powerConsumed = 0f

            if (be.isExploding()) {
                world.removeBlock(pos, false)
                world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 3.5f, World.ExplosionSourceType.TNT)
                return
            }

            be.markDirty()
            world.updateListeners(pos, state, state, net.minecraft.block.Block.NOTIFY_ALL)
        }
    }
}
