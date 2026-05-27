package com.fou.client.mixin;

import com.fou.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InGameHud.class)
public class CycleTotemPopMixin {

    private static final Identifier CYCLE_TOTEM_TEXTURE =
            Identifier.of("fou", "textures/item/totem_of_cycle.png");

    @ModifyArg(
        method = "renderMiscOverlays",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/InGameHud;renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V"
        ),
        index = 1
    )
    private Identifier fou$swapTotemOverlayTexture(Identifier original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            for (Hand hand : Hand.values()) {
                ItemStack stack = client.player.getStackInHand(hand);
                if (stack.isOf(ModItems.INSTANCE.getTOTEM_OF_CYCLE())) {
                    return CYCLE_TOTEM_TEXTURE;
                }
            }
        }
        return original;
    }
}
