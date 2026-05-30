package com.fou.registry

import com.fou.FouMod
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object ModItemGroups {

    val FOU_GROUP = Registry.register(
        Registries.ITEM_GROUP,
        Identifier.of(FouMod.MOD_ID, "main"),
        FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.fou.main"))
            .icon { ItemStack(ModItems.REPAIR_STATION) }
            .entries { _, entries ->
                entries.add(ModItems.REPAIR_STATION)
                entries.add(ModItems.POWER_GENERATOR)
                entries.add(ModItems.VOLTAGE_STABILIZER)
                entries.add(ModItems.LINKER)
                entries.add(ModItems.TOTEM_OF_CYCLE)
                entries.add(ModItems.DRILL)
                entries.add(ModItems.CHISEL)
                entries.add(ModItems.LEAF_SHEAR)
            }
            .build()
    )

    fun initialize() {
        FouMod.LOGGER.info("[FOU] Item Groups registered.")
    }
}
