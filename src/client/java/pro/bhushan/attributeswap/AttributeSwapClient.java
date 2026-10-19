package pro.bhushan.attributeswap;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class AttributeSwapClient implements ClientModInitializer {
	public static final String MOD_ID = "attributeswap";
	/** Slot that gets the default key (index 3 = hotbar slot 4). */
	public static final int DEFAULT_SLOT = 3;
	public static final KeyMapping[] SWAP_KEYS = new KeyMapping[9];

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		for (int i = 0; i < 9; i++) {
			int key = i == DEFAULT_SLOT ? InputConstants.KEY_R : InputConstants.UNKNOWN.getValue();
			SWAP_KEYS[i] = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.attributeswap.slot." + (i + 1), InputConstants.Type.KEYBOARD, key, category, i));
		}
	}
}
