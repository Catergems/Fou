package com.fou.registry

import com.fou.FouMod
import com.fou.block.RepairStationBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier

object ModBlocks {

    // Pass a lambda function (Settings -> Block) instead of instantiating the block early
    val REPAIR_STATION: Block = register("repair_station") { settings ->
        RepairStationBlock(
            settings
                .strength(3.5f, 6.0f)
                .sounds(BlockSoundGroup.METAL)
                .requiresTool()
        )
    }

    // Helper method handles the creation of the key and passes the pre-keyed settings to your block
    private fun register(name: String, blockFactory: (AbstractBlock.Settings) -> Block): Block {
        val id = Identifier.of(FouMod.MOD_ID, name)
        val key = RegistryKey.of(RegistryKeys.BLOCK, id)

        // Create the settings object and assign the registry key right away
        val settings = AbstractBlock.Settings.create().registryKey(key)

        // Build the block instance using the factory lambda
        val block = blockFactory(settings)

        return Registry.register(Registries.BLOCK, key, block)
    }

    fun initialize() {
        FouMod.LOGGER.info("[FOU] Blocks registered.")
    }
}