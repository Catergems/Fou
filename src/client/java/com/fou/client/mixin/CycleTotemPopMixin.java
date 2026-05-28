package com.fou.client.mixin;

import com.fou.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(InGameOverlayRenderer.class)
public class CycleTotemPopMixin {

    @ModifyVariable(
        method = "setFloatingItem",
        at = @At("HEAD"),
        argsOnly = true
    )
    private ItemStack fou$useCycleTotemForPop(ItemStack original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            for (Hand hand : Hand.values()) {
                ItemStack stack = client.player.getStackInHand(hand);
                if (stack.isOf(ModItems.INSTANCE.getTOTEM_OF_CYCLE())) {
                    return stack;
                }
            }
        }
        return original;
    }
}
