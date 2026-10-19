package pro.bhushan.attributeswap.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.bhushan.attributeswap.AttributeSwap;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "handleKeybinds", at = @At("HEAD"))
	private void attributeswap$handleKeys(CallbackInfo ci) {
		AttributeSwap.handleKeys((Minecraft) (Object) this);
	}
}
