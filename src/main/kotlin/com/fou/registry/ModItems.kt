package com.fou.registry

import com.fou.FouMod
import com.fou.item.DrillItem
import com.fou.item.TotemOfCycleItem
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier

object ModItems {

    // 1. For the BlockItem, pass the block and the keyed settings lambda
    val REPAIR_STATION: Item = register("repair_station") { settings ->
        BlockItem(ModBlocks.REPAIR_STATION, settings)
    }

    // 2. For your custom item, pass your class and the keyed settings lambda
    val TOTEM_OF_CYCLE: Item = register("totem_of_cycle") { settings ->
        TotemOfCycleItem(settings.maxCount(1))
    }

    val DRILL: Item = register("drill") { settings ->
        DrillItem(settings.maxCount(1))
    }

    // Updated helper to intercept settings and attach the RegistryKey before instantiation
    private fun register(name: String, itemFactory: (Item.Settings) -> Item): Item {
        val id = Identifier.of(FouMod.MOD_ID, name)
        val key = RegistryKey.of(RegistryKeys.ITEM, id)

        // Create settings and bind the registry key instantly
        val settings = Item.Settings().registryKey(key)

        // Construct the item instance using our factory rule
        val item = itemFactory(settings)

        return Registry.register(Registries.ITEM, key, item)
    }

    fun initialize() {
        FouMod.LOGGER.info("[FOU] Items registered.")
    }
}