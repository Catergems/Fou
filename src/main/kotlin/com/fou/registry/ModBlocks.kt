package com.fou.registry

import com.fou.FouMod
import com.fou.block.CoolantBlock
import com.fou.block.CrusherBlock
import com.fou.block.PowerGeneratorBlock
import com.fou.block.RepairStationBlock
import com.fou.block.VoltageStabilizerBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier

object ModBlocks {

    val REPAIR_STATION: Block = register("repair_station") { settings ->
        RepairStationBlock(settings.strength(3.5f, 6.0f).sounds(BlockSoundGroup.METAL).requiresTool())
    }

    val POWER_GENERATOR: Block = register("power_generator") { settings ->
        PowerGeneratorBlock(settings.strength(3.5f, 6.0f).sounds(BlockSoundGroup.METAL).requiresTool())
    }

    val VOLTAGE_STABILIZER: Block = register("voltage_stabilizer") { settings ->
        VoltageStabilizerBlock(settings.strength(3.0f, 6.0f).sounds(BlockSoundGroup.METAL).requiresTool())
    }

    val COOLANT_BLOCK: Block = register("coolant_block") { settings ->
        CoolantBlock(settings.strength(2.5f, 6.0f).sounds(BlockSoundGroup.METAL).requiresTool())
    }

    val CRUSHER: Block = register("crusher") { settings ->
        CrusherBlock(settings.strength(4.0f, 6.0f).sounds(BlockSoundGroup.METAL).requiresTool())
    }

    private fun register(name: String, blockFactory: (AbstractBlock.Settings) -> Block): Block {
        val id = Identifier.of(FouMod.MOD_ID, name)
        val key = RegistryKey.of(RegistryKeys.BLOCK, id)
        val settings = AbstractBlock.Settings.create().registryKey(key)
        val block = blockFactory(settings)
        return Registry.register(Registries.BLOCK, key, block)
    }

    fun initialize() {
        FouMod.LOGGER.info("[FOU] Blocks registered.")
    }
}
