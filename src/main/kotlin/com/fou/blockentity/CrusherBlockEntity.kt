package com.fou.blockentity

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

class CrusherBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.CRUSHER, pos, state) {

    val inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(2, ItemStack.EMPTY) // 0=input, 1=output
    var progress: Int = 0
    var inputWatts: Float = 0f

    val isRunning: Boolean get() = !inventory[0].isEmpty && inputWatts >= MIN_WATTS

    override fun readData(view: ReadView) {
        super.readData(view)
        progress = view.getInt("Progress", 0)
        view.read("InputItem", ItemStack.CODEC).ifPresent { inventory[0] = it }
        view.read("OutputItem", ItemStack.CODEC).ifPresent { inventory[1] = it }
    }

    override fun writeData(view: WriteView) {
        super.writeData(view)
        view.putInt("Progress", progress)
        if (!inventory[0].isEmpty) view.put("InputItem", ItemStack.CODEC, inventory[0])
        if (!inventory[1].isEmpty) view.put("OutputItem", ItemStack.CODEC, inventory[1])
    }

    companion object {
        const val MIN_WATTS  = 100f
        const val MAX_WATTS  = 150f
        const val CRUSH_TIME = 100 // 5 seconds

        fun tick(world: World, pos: BlockPos, state: BlockState, be: CrusherBlockEntity) {
            if (world.isClient) return

            if (!be.isRunning) {
                be.progress = 0
                be.inputWatts = 0f
                be.markDirty()
                return
            }

            be.progress++
            if (be.progress >= CRUSH_TIME) {
                be.progress = 0
                crush(be)
                be.markDirty()
                world.updateListeners(pos, state, state, net.minecraft.block.Block.NOTIFY_ALL)
            }
            be.inputWatts = 0f
            be.markDirty()
        }

        private fun crush(be: CrusherBlockEntity) {
            val input = be.inventory[0]
            val drops = getDrops(input) ?: return

            // Try to add to output slot
            drops.forEach { drop ->
                if (be.inventory[1].isEmpty) {
                    be.inventory[1] = drop.copy()
                } else if (be.inventory[1].isOf(drop.item) &&
                    be.inventory[1].count + drop.count <= be.inventory[1].maxCount) {
                    be.inventory[1].increment(drop.count)
                }
            }
            input.decrement(1)
        }

        private fun getDrops(stack: ItemStack): List<ItemStack>? {
            return when {
                // Stone
                stack.isOf(Items.STONE) || stack.isOf(Items.COBBLESTONE) -> buildList {
                    add(ItemStack(Items.GRAVEL, 1))
                    add(ItemStack(Items.FLINT, Random.nextInt(1, 3)))
                    if (Random.nextFloat() < 0.05f) add(ItemStack(Items.DIAMOND, 1))
                    if (Random.nextFloat() < 0.3f) add(ItemStack(Items.IRON_NUGGET, Random.nextInt(1, 3)))
                }
                // Gravel
                stack.isOf(Items.GRAVEL) -> buildList {
                    add(ItemStack(Items.SAND, 1))
                    add(ItemStack(Items.FLINT, Random.nextInt(1, 3)))
                    if (Random.nextFloat() < 0.2f) add(ItemStack(Items.IRON_NUGGET, 1))
                }
                // Sand
                stack.isOf(Items.SAND) -> listOf(ItemStack(Items.GLASS, 1))
                else -> null
            }
        }
    }
}
