package pro.bhushan.attributeswap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import pro.bhushan.attributeswap.config.AttributeSwapConfig;
import pro.bhushan.attributeswap.mixin.MinecraftInvoker;
import pro.bhushan.attributeswap.mixin.MultiPlayerGameModeInvoker;

/**
 * Attribute swap (MC-28289): select the target slot and attack in the same tick.
 * The server only refreshes held-item attributes in the player tick, so the hit uses the
 * previous item's damage and attack speed, and the target item's enchantments and effects.
 *
 * Triple swap: when nothing is in normal reach, the target is picked with the attack range
 * of a spear in the hotbar (4.5 blocks), as if swapping through it. That range only decides
 * the client's target; the server accepts the hit as long as it is within the attacking
 * item's range plus 3 blocks. The spear slot is never selected, so the server never sees it.
 */
public final class AttributeSwap {
	private AttributeSwap() {}

	/** The client's connection while a swap is being sent, else null. Only touched on the client thread. */
	private static Connection batching;

	/** True for packets sent on the client's connection during a swap: they are written but not flushed yet. */
	public static boolean isBatching(Connection connection) {
		return batching != null && connection == batching;
	}

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
		AttributeSwapConfig config = AttributeSwapConfig.get();
		if (!config.enabled || player == null || mc.gameMode == null || mc.hitResult == null) return;
		if (player.isSpectator() || player.isUsingItem()) return;

		Inventory inventory = player.getInventory();
		int original = inventory.getSelectedSlot();
		boolean piercing = inventory.getItem(target).has(DataComponents.PIERCING_WEAPON);
		HitResult reachHit = null;
		// Only act on a hit that sends a server attack: an entity, or a spear jab (which needs no target).
		if (mc.hitResult.getType() != HitResult.Type.ENTITY && !piercing) {
			if (config.spearReach) reachHit = spearReachHit(mc, player, original, config.reachSlot);
			if (reachHit == null) return;
		}

		HitResult crosshair = mc.hitResult;
		ClientPacketListener listener = mc.getConnection();
		// Write the slot change, attack and return without flushing, then flush once, so they reach the
		// server in one network read and are handled in the same server tick.
		batching = listener == null ? null : listener.getConnection();
		try {
			inventory.setSelectedSlot(target);
			if (reachHit != null) mc.hitResult = reachHit;
			// Vanilla attack path: sends SetCarriedItem(target) then the attack in this tick.
			((MinecraftInvoker) mc).attributeswap$startAttack();
			mc.hitResult = crosshair;
			if (config.returnToSlot && original != target) {
				inventory.setSelectedSlot(original);
				// Send the return now so the server never ticks with the target item selected.
				((MultiPlayerGameModeInvoker) mc.gameMode).attributeswap$ensureHasSentCarriedItem();
			}
		} finally {
			mc.hitResult = crosshair;
			Connection connection = batching;
			batching = null;
			if (connection != null) connection.flushChannel();
		}
	}

	/**
	 * The entity vanilla would target if the reach item were held, or null.
	 * reachSlot 0 means any hotbar slot, using the item with the longest attack range.
	 */
	private static HitResult spearReachHit(Minecraft mc, LocalPlayer player, int held, int reachSlot) {
		if (mc.getCameraEntity() != player) return null;
		Inventory inventory = player.getInventory();
		AttackRange best = null;
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			if (slot == held || (reachSlot >= 1 && reachSlot <= 9 && slot != reachSlot - 1)) continue;
			AttackRange range = inventory.getItem(slot).get(DataComponents.ATTACK_RANGE);
			if (range != null && (best == null || range.effectiveMaxRange(player) > best.effectiveMaxRange(player))) best = range;
		}
		if (best == null) return null;
		// Same pick as LocalPlayer.raycastHitResult with that item held, at the current tick's position.
		HitResult hit = best.getClosesetHit(player, 1.0F, EntitySelector.CAN_BE_PICKED);
		return hit instanceof EntityHitResult ? hit : null;
	}
}
