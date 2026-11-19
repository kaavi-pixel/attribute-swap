package pro.bhushan.attributeswap.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class AttributeSwapConfigScreen extends Screen {
	private final Screen parent;
	private boolean enabled;
	private boolean returnToSlot;
	private boolean spearReach;
	private int reachSlot;

	public AttributeSwapConfigScreen(Screen parent) {
		super(Component.translatable("attributeswap.config.title"));
		this.parent = parent;
		this.enabled = AttributeSwapConfig.get().enabled;
		this.returnToSlot = AttributeSwapConfig.get().returnToSlot;
		this.spearReach = AttributeSwapConfig.get().spearReach;
		this.reachSlot = AttributeSwapConfig.get().reachSlot;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - 100;
		int y = this.height / 2 - 60;
		this.addRenderableWidget(CycleButton.onOffBuilder(this.enabled)
			.create(x, y, 200, 20, Component.translatable("attributeswap.config.enabled"), (button, value) -> this.enabled = value));
		this.addRenderableWidget(CycleButton.onOffBuilder(this.returnToSlot)
			.create(x, y + 24, 200, 20, Component.translatable("attributeswap.config.return"), (button, value) -> this.returnToSlot = value));
		this.addRenderableWidget(CycleButton.onOffBuilder(this.spearReach)
			.create(x, y + 48, 200, 20, Component.translatable("attributeswap.config.spear_reach"), (button, value) -> this.spearReach = value));
		this.addRenderableWidget(CycleButton.<Integer>builder(
				slot -> slot == 0 ? Component.translatable("attributeswap.config.reach_slot.any") : Component.literal(String.valueOf(slot)), this.reachSlot)
			.withValues(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
			.create(x, y + 72, 200, 20, Component.translatable("attributeswap.config.reach_slot"), (button, value) -> this.reachSlot = value));
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(x, y + 104, 200, 20)
			.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 84, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		AttributeSwapConfig config = AttributeSwapConfig.get();
		config.enabled = this.enabled;
		config.returnToSlot = this.returnToSlot;
		config.spearReach = this.spearReach;
		config.reachSlot = this.reachSlot;
		config.save();
		this.minecraft.gui.setScreen(this.parent);
	}
}
