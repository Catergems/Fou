package com.fou.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.EntityStatuses
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World

class TotemOfCycleItem(settings: Settings) : Item(settings) {

    companion object {
        const val COOLDOWN_TICKS        = 1200   // 60 seconds
        private const val NIGHT_START   = 13000L
        private const val DAY_LENGTH    = 24000L
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return ActionResult.PASS

        if (user.itemCooldownManager.isCoolingDown(stack)) {
            user.sendMessage(Text.literal("§cTotem of Cycle is on cooldown! §7(60s per use)"), true)
            return ActionResult.FAIL
        }

        user.itemCooldownManager.set(stack, COOLDOWN_TICKS)
        world.sendEntityStatus(user, EntityStatuses.USE_TOTEM_OF_UNDYING)

        if (world is ServerWorld) {
            val current = world.timeOfDay % DAY_LENGTH
            val skip    = if (current < NIGHT_START) NIGHT_START - current else DAY_LENGTH - current
            world.setTimeOfDay(world.timeOfDay + skip)

            val phase = if (current < NIGHT_START) "§9Night" else "§eDay"
            world.server?.playerManager?.playerList?.forEach { p ->
                p.sendMessage(Text.literal("§6[Totem of Cycle] §fSkipped to $phase §7by §f${user.name.string}"), false)
            }
        }

        return ActionResult.SUCCESS
    }
}
