package com.fou.registry

import com.fou.FouMod
import com.fou.blockentity.CrusherBlockEntity
import com.fou.blockentity.PowerGeneratorBlockEntity
import com.fou.blockentity.RepairStationBlockEntity
import com.fou.blockentity.VoltageStabilizerBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ModBlockEntities {

    val REPAIR_STATION: BlockEntityType<RepairStationBlockEntity> = register(
        "repair_station",
        FabricBlockEntityTypeBuilder.create(::RepairStationBlockEntity, ModBlocks.REPAIR_STATION).build()
    )

    val POWER_GENERATOR: BlockEntityType<PowerGeneratorBlockEntity> = register(
        "power_generator",
        FabricBlockEntityTypeBuilder.create(::PowerGeneratorBlockEntity, ModBlocks.POWER_GENERATOR).build()
    )

    val VOLTAGE_STABILIZER: BlockEntityType<VoltageStabilizerBlockEntity> = register(
        "voltage_stabilizer",
        FabricBlockEntityTypeBuilder.create(::VoltageStabilizerBlockEntity, ModBlocks.VOLTAGE_STABILIZER).build()
    )

    val CRUSHER: BlockEntityType<CrusherBlockEntity> = register(
        "crusher",
        FabricBlockEntityTypeBuilder.create(::CrusherBlockEntity, ModBlocks.CRUSHER).build()
    )

    private fun <T : net.minecraft.block.entity.BlockEntity> register(
        name: String, type: BlockEntityType<T>
    ): BlockEntityType<T> =
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(FouMod.MOD_ID, name), type)

    fun initialize() {
        FouMod.LOGGER.info("[FOU] Block Entities registered.")
    }
}
