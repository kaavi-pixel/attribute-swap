# Attribute Swap 1.0.0

Client-side Fabric mod for Minecraft 26.3 (Java 25, Fabric Loader 0.19.5+). Requires Fabric API (for the key bindings). Mod Menu (21.0.0 for 26.3) is optional and adds the settings button.

## What it does

Press the swap key while aiming at a mob or player. The mod hits with the item in the swap slot, but the hit keeps the damage and attack speed of the item you're holding. Then it switches you back in the same tick.

Example: diamond sword in hand, Fire Aspect wooden sword in slot 4. The hit does the diamond sword's 7 damage and sets the target on fire. Durability comes off the wooden sword.

The swap slot's item supplies its enchantments, mace smash damage, spear jab, and durability use. The held item's attack damage and attack speed (cooldown) are used.

## Keys

Options > Controls > Key Binds > Attribute Swap. There is one entry per hotbar slot, "Swap attack: slot 1" to "Swap attack: slot 9".

- Slot 4 is bound to R by default. The other slots are unbound.
- To swap with a different slot, unbind slot 4 and bind the slot you want. You can bind several slots at once.
- The mod never presses or reads your hotbar keys, so it works whatever keys you use for the hotbar, including none.

The key does nothing if you're aiming at a block or nothing, unless the swap slot holds a spear, since a spear jab doesn't need a target. It also does nothing in spectator mode, while using an item (eating, blocking, drawing a bow), or with a screen open. If the swap slot is the slot you're already on, it's a normal attack.

## Settings

Mod Menu > Attribute Swap. Saved in `config/attributeswap.json`.

- Attribute swap: ON/OFF. OFF makes the swap keys do nothing. Default ON.
- Return to original slot: ON/OFF. Default ON. With it OFF you stay on the swap slot after the hit, like doing it by hand.

## How it works

This is Mojang bug MC-28289. When you change hotbar slot, the server only sets the slot. It updates your held-item damage and attack speed later, in your player tick. An attack that arrives in the same batch of packets as the slot change uses the old item's damage and attack speed, and the new item's enchantments.

On a key press the mod runs inside `Minecraft.handleKeybinds`, before vanilla's own hotbar and attack keys:

1. Select the swap slot.
2. Run vanilla's attack (`Minecraft.startAttack`), which sends the slot change and then the attack packet.
3. Select the original slot again and send that slot change right away.

Because all three packets go out in one tick, the server never ticks with the swap item selected. Your held item's attributes are never replaced, and its cooldown isn't reset by an item change.

## Notes

- Mojang calls this unsupported. A fix appeared in a 26.2 pre-release and was then reverted, so a later version could patch it.
- Paper servers block attribute swapping by default. Other servers and anti-cheats may too, and many servers count it as an exploit. Check the rules before using it in multiplayer.

## Testing

`./gradlew runClientGameTest` runs a real 26.3 client against an integrated and a dedicated server. A no-AI cow with 1000 health and fire resistance is the target. The tests cover:

- A normal hit with each item, as a baseline.
- The swap key with Fire Aspect and Sharpness V items.
- Slot 7 on a rebound key with every vanilla hotbar key unbound.
- An unbound key, the mod turned off, aiming at a block, and swapping to the slot you're already on.
- Return turned off.
- Spear jabs on a target and at the sky (singleplayer only, because the test server shares the client process and spear damage types can't be sent between them there).
- A mace smash from a 5.7-block fall.

They check the server-side damage, fire, durability of each item, and the server's selected slot on every tick. On headless Linux, run them under Xvfb with `SDL_VIDEO_FORCE_EGL=1`.
