package pro.bhushan.attributeswap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.HitResult;
import pro.bhushan.attributeswap.config.AttributeSwapConfig;
import pro.bhushan.attributeswap.mixin.MinecraftInvoker;
import pro.bhushan.attributeswap.mixin.MultiPlayerGameModeInvoker;

/**
 * Attribute swap (MC-28289): select the target slot and attack in the same tick.
 * The server only refreshes held-item attributes in the player tick, so the hit uses the
 * previous item's damage and attack speed, and the target item's enchantments and effects.
 */
public final class AttributeSwap {
	private AttributeSwap() {}

	/** Called at the start of Minecraft.handleKeybinds, before vanilla hotbar and attack keys. */
	public static void handleKeys(Minecraft mc) {
		for (int slot = 0; slot < AttributeSwapClient.SWAP_KEYS.length; slot++) {
			while (AttributeSwapClient.SWAP_KEYS[slot].consumeClick()) {
				swap(mc, slot);
			}
		}
	}

	public static void swap(Minecraft mc, int target) {
		LocalPlayer player = mc.player;
		if (!AttributeSwapConfig.get().enabled || player == null || mc.gameMode == null || mc.hitResult == null) return;
		if (player.isSpectator() || player.isUsingItem()) return;

		Inventory inventory = player.getInventory();
		boolean piercing = inventory.getItem(target).has(DataComponents.PIERCING_WEAPON);
		// Only act on a hit that sends a server attack: an entity, or a spear jab (which needs no target).
		if (mc.hitResult.getType() != HitResult.Type.ENTITY && !piercing) return;

		int original = inventory.getSelectedSlot();
		inventory.setSelectedSlot(target);
		// Vanilla attack path: sends SetCarriedItem(target) then the attack in this tick.
		((MinecraftInvoker) mc).attributeswap$startAttack();
		if (AttributeSwapConfig.get().returnToSlot && original != target) {
			inventory.setSelectedSlot(original);
			// Send the return now so the server never ticks with the target item selected.
			((MultiPlayerGameModeInvoker) mc.gameMode).attributeswap$ensureHasSentCarriedItem();
		}
	}
}
