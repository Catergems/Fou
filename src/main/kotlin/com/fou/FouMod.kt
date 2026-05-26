package com.fou

import com.fou.block.RepairStationBlock
import com.fou.registry.ModBlockEntities
import com.fou.registry.ModBlocks
import com.fou.registry.ModItemGroups
import com.fou.registry.ModItems
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object FouMod : ModInitializer {

    const val MOD_ID = "fou"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        ModBlocks.initialize()
        ModItems.initialize()
        ModBlockEntities.initialize()
        ModItemGroups.initialize()
        RepairStationBlock.registerUseCallback()
        LOGGER.info("[Future of Utility] Initialized!")
    }
}
