package com.fou.registry

import com.fou.FouMod
import com.fou.item.ChiselItem
import com.fou.item.CoreMatterItem
import com.fou.item.DrillItem
import com.fou.item.LeafShearItem
import com.fou.item.LinkerItem
import com.fou.item.TotemOfCycleItem
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier

object ModItems {

    val REPAIR_STATION: Item = register("repair_station") { settings ->
        BlockItem(ModBlocks.REPAIR_STATION, settings)
    }

    val TOTEM_OF_CYCLE: Item = register("totem_of_cycle") { settings ->
        TotemOfCycleItem(settings.maxCount(1))
    }

    val DRILL: Item = register("drill") { settings ->
        DrillItem(settings.maxCount(1))
    }

    val POWER_GENERATOR: Item = register("power_generator") { settings ->
        BlockItem(ModBlocks.POWER_GENERATOR, settings)
    }

    val VOLTAGE_STABILIZER: Item = register("voltage_stabilizer") { settings ->
        BlockItem(ModBlocks.VOLTAGE_STABILIZER, settings)
    }

    val CHISEL: Item = register("chisel") { settings ->
        ChiselItem(settings.maxCount(1).maxDamage(256))
    }

    val CORE_MATTER: Item = register("core_matter") { settings ->
        CoreMatterItem(settings.maxCount(64))
    }

    val LEAF_SHEAR: Item = register("leaf_shear") { settings ->
        LeafShearItem(settings.maxCount(1))
    }

    val LINKER: Item = register("linker") { settings ->
        LinkerItem(settings.maxCount(1))
    }

    val CRUSHER: Item = register("crusher") { settings ->
        BlockItem(ModBlocks.CRUSHER, settings)
    }

    private fun register(name: String, itemFactory: (Item.Settings) -> Item): Item {
        val id = Identifier.of(FouMod.MOD_ID, name)
        val key = RegistryKey.of(RegistryKeys.ITEM, id)
        val settings = Item.Settings().registryKey(key)
        val item = itemFactory(settings)
        return Registry.register(Registries.ITEM, key, item)
    }

    fun initialize() {
        FouMod.LOGGER.info("[FOU] Items registered.")
    }
}
