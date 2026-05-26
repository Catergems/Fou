package com.fou.registry

import com.fou.FouMod
import com.fou.blockentity.RepairStationBlockEntity
// Notice the backticks around `object` here:
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ModBlockEntities {

    val REPAIR_STATION: BlockEntityType<RepairStationBlockEntity> =
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(FouMod.MOD_ID, "repair_station"),
            FabricBlockEntityTypeBuilder.create(
                ::RepairStationBlockEntity,
                ModBlocks.REPAIR_STATION
            ).build()
        )

    fun initialize() {
        FouMod.LOGGER.info("[FOU] Block Entities registered.")
    }
}