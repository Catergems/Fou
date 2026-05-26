package com.fou.client.mixin;

import com.fou.registry.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayNetworkHandler.class)
public class CycleTotemPopMixin {
	@Inject(method = "getActiveDeathProtector", at = @At("HEAD"), cancellable = true)
	private static void fou$useCycleTotemPopItem(PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
		for (Hand hand : Hand.values()) {
			if (player.getStackInHand(hand).contains(DataComponentTypes.DEATH_PROTECTION)) {
				return;
			}
		}

		for (Hand hand : Hand.values()) {
			ItemStack stack = player.getStackInHand(hand);
			if (stack.isOf(ModItems.INSTANCE.getTOTEM_OF_CYCLE())) {
				cir.setReturnValue(stack);
				return;
			}
		}
	}
}
