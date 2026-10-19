package pro.bhushan.attributeswap.test;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import pro.bhushan.attributeswap.AttributeSwapClient;
import pro.bhushan.attributeswap.config.AttributeSwapConfig;
import pro.bhushan.attributeswap.config.AttributeSwapModMenu;

@SuppressWarnings("UnstableApiUsage")
public class AttributeSwapClientGameTest implements FabricClientGameTest {
	private static final List<String> results = new ArrayList<>() {
		@Override
		public boolean add(String r) {
			System.out.println("ATTRSWAP-LIVE " + r);
			return super.add(r);
		}
	};
	private static final List<Integer> serverSlots = new ArrayList<>();
	private static volatile boolean sampling;
	private static boolean spear;

	private static final String SWORD = "diamond_sword";
	private static final String FIRE = "wooden_sword[enchantments={fire_aspect:2}]";
	private static final String SHARP = "stone_sword[enchantments={sharpness:5}]";

	@Override
	public void runTest(ClientGameTestContext ctx) {
		ServerTickEvents.END_SERVER_TICK.register(s -> {
			if (!sampling) return;
			for (ServerPlayer p : s.getPlayerList().getPlayers()) {
				synchronized (serverSlots) { serverSlots.add(p.getInventory().getSelectedSlot()); }
			}
		});

		results.add("default key slot4=" + ctx.computeOnClient(mc -> AttributeSwapClient.SWAP_KEYS[3].saveString())
			+ " slot1=" + ctx.computeOnClient(mc -> AttributeSwapClient.SWAP_KEYS[0].saveString())
			+ " category=" + ctx.computeOnClient(mc -> AttributeSwapClient.SWAP_KEYS[3].getCategory().label().getString())
			+ " name=" + ctx.computeOnClient(mc -> net.minecraft.network.chat.Component.translatable(AttributeSwapClient.SWAP_KEYS[3].getName()).getString())
			+ " conflictsWithVanilla=" + ctx.computeOnClient(mc -> {
				for (KeyMapping k : mc.options.keyMappings) {
					if (k != AttributeSwapClient.SWAP_KEYS[3] && k.same(AttributeSwapClient.SWAP_KEYS[3])) return k.getName();
				}
				return "none";
			}));

		ctx.runOnClient(mc -> mc.gui.setScreen(new AttributeSwapModMenu().getModConfigScreenFactory().create(null)));
		ctx.waitTicks(5);
		ctx.takeScreenshot("attributeswap-config");
		ctx.runOnClient(mc -> mc.gui.screen().onClose());
		ctx.waitTicks(2);
		results.add("config file exists=" + java.nio.file.Files.exists(
			net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("attributeswap.json")));

		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			scenarios(ctx, sp.getServer(), sp.getConnection(), "singleplayer");
		}
		try (TestDedicatedServerContext server = ctx.worldBuilder().createServer();
			 TestDedicatedServerConnection conn = server.connect()) {
			conn.waitForChunksRender();
			scenarios(ctx, server, conn, "dedicated");
		}

		System.out.println("ATTRSWAP-RESULTS-BEGIN");
		results.forEach(r -> System.out.println("ATTRSWAP " + r));
		System.out.println("ATTRSWAP-RESULTS-END");
	}

	private static void scenarios(ClientGameTestContext ctx, TestServerContext server, TestServerConnection conn, String label) {
		AttributeSwapConfig cfg = AttributeSwapConfig.get();
		cfg.enabled = true;
		cfg.returnToSlot = true;
		setKey(ctx, 3, InputConstants.KEY_R);
		setKey(ctx, 6, -1);
		server.runCommand("forceload add 90 90 115 115");
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("difficulty peaceful");
		server.runCommand("gamemode survival @a");
		server.runCommand("fill 95 149 95 110 149 106 stone");
		server.runCommand("fill 95 150 95 110 160 106 air");
		ctx.waitTicks(10);

		hit(ctx, server, conn, label + " A control: left click, diamond sword", () -> ctx.getInput().pressKey(o -> o.keyAttack));
		hit(ctx, server, conn, label + " B control: left click, slot 4 fire wooden sword selected", () -> {
			ctx.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(3));
			ctx.waitTicks(25);
			ctx.getInput().pressKey(o -> o.keyAttack);
		});
		hit(ctx, server, conn, label + " C swap key R -> slot 4 (expect 7 dmg, fire, wooden durability 1)", () -> ctx.getInput().pressKey(InputConstants.KEY_R));

		// Rebind: slot 7 on J, slot 4 unbound, all vanilla hotbar keys unbound.
		setKey(ctx, 3, -1);
		setKey(ctx, 6, InputConstants.KEY_J);
		ctx.runOnClient(mc -> {
			for (KeyMapping k : mc.options.keyHotbarSlots) k.setKey(InputConstants.UNKNOWN);
			KeyMapping.resetMapping();
		});
		hit(ctx, server, conn, label + " D rebound J -> slot 7, hotbar keys unbound (expect 10 dmg, stone durability 1)", () -> ctx.getInput().pressKey(InputConstants.KEY_J));
		hit(ctx, server, conn, label + " E R after unbinding (expect nothing)", () -> ctx.getInput().pressKey(InputConstants.KEY_R));
		ctx.runOnClient(mc -> {
			for (int i = 0; i < 9; i++) mc.options.keyHotbarSlots[i].setKey(mc.options.keyHotbarSlots[i].getDefaultKey());
			KeyMapping.resetMapping();
		});
		setKey(ctx, 6, -1);
		setKey(ctx, 3, InputConstants.KEY_R);

		cfg.returnToSlot = false;
		hit(ctx, server, conn, label + " F return off (expect 7 dmg, fire, ends on slot 4)", () -> ctx.getInput().pressKey(InputConstants.KEY_R));
		cfg.returnToSlot = true;

		cfg.enabled = false;
		hit(ctx, server, conn, label + " G disabled (expect nothing)", () -> ctx.getInput().pressKey(InputConstants.KEY_R));
		cfg.enabled = true;

		hit(ctx, server, conn, label + " H aim at block (expect nothing)", () -> {
			ctx.getInput().lookAt(new net.minecraft.core.BlockPos(100, 149, 103));
			ctx.waitTicks(2);
			ctx.getInput().pressKey(InputConstants.KEY_R);
		});

		hit(ctx, server, conn, label + " I already on slot 4 (expect normal 4 dmg, fire)", () -> {
			ctx.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(3));
			ctx.waitTicks(25);
			ctx.getInput().pressKey(InputConstants.KEY_R);
		});


		maceHit(ctx, server, conn, label + " J control: mace falling", "mace", false);
		maceHit(ctx, server, conn, label + " K swap sword->mace falling", SWORD, true);
		ctx.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
		// Spears only in singleplayer: the test's dedicated server shares the client JVM, and spear items hold a
		// DAMAGE_TYPE holder re-bound to the client's registries on join, so the server can't encode it there.
		if (!label.equals("singleplayer")) return;
		spear = true;
		hit(ctx, server, conn, label + " L control: spear jab", () -> {
			ctx.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(3));
			ctx.waitTicks(25);
			ctx.getInput().pressKey(o -> o.keyAttack);
		});
		hit(ctx, server, conn, label + " M swap sword->spear on cow", () -> ctx.getInput().pressKey(InputConstants.KEY_R));
		hit(ctx, server, conn, label + " N swap sword->spear at sky (jab, no target)", () -> {
			ctx.getInput().lookAt(0f, -90f);
			ctx.waitTicks(2);
			ctx.getInput().pressKey(InputConstants.KEY_R);
		});
		spear = false;
	}

	private static void setKey(ClientGameTestContext ctx, int slot, int key) {
		ctx.runOnClient(mc -> {
			AttributeSwapClient.SWAP_KEYS[slot].setKey(key < 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYBOARD.getOrCreate(key));
			KeyMapping.resetMapping();
		});
	}

	private static void setup(ClientGameTestContext ctx, TestServerContext server, TestServerConnection conn, String main, double px, double py, double pz) {
		server.runCommand("kill @e[type=cow]");
		server.runCommand("summon cow 102.5 150 100.5 {NoAI:1b,Tags:[\"t\"],attributes:[{id:\"minecraft:max_health\",base:1000}],Health:1000f,active_effects:[{id:\"minecraft:fire_resistance\",duration:-1}]}");
		server.runCommand("clear @a");
		server.runCommand("effect clear @a");
		server.runCommand("item replace entity @a hotbar.0 with " + main);
		server.runCommand("item replace entity @a hotbar.3 with " + (main.equals("mace") ? SWORD : main.equals(SWORD) && py > 150 ? "mace" : spear ? "iron_spear" : FIRE));
		server.runCommand("item replace entity @a hotbar.6 with " + SHARP);
		server.runCommand("tp @a " + px + " " + py + " " + pz + " facing entity @e[type=cow,limit=1] eyes");
		ctx.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
		ctx.waitTicks(30);
		conn.waitForClientboundPackets();
		ctx.getInput().lookAt(COW_BLOCK);
		ctx.waitTicks(2);
	}

	private static final net.minecraft.core.BlockPos COW_BLOCK = new net.minecraft.core.BlockPos(102, 150, 100);

	private static void hit(ClientGameTestContext ctx, TestServerContext server, TestServerConnection conn, String name, Runnable action) {
		setup(ctx, server, conn, SWORD, 100.5, 150, 100.5);
		String before = state(ctx, server, conn);
		synchronized (serverSlots) { serverSlots.clear(); }
		sampling = true;
		action.run();
		ctx.waitTicks(10);
		conn.waitForClientboundPackets();
		ctx.waitTicks(2);
		sampling = false;
		String slots;
		synchronized (serverSlots) { slots = serverSlots.toString(); }
		if (slots.length() > 120) slots = slots.substring(0, 120) + "...";
		results.add(name + " | before " + before + " | after " + state(ctx, server, conn) + " | serverSlotPerTick " + slots);
	}

	private static void maceHit(ClientGameTestContext ctx, TestServerContext server, TestServerConnection conn, String name, String main, boolean useKey) {
		setup(ctx, server, conn, main, 100.5, 150.0001, 100.5);
		String before = state(ctx, server, conn);
		server.runCommand("tp @a 100.5 157 100.5");
		synchronized (serverSlots) { serverSlots.clear(); }
		sampling = true;
		String at = "never in reach";
		for (int t = 0; t < 60; t++) {
			ctx.getInput().lookAt(COW_BLOCK);
			ctx.waitTicks(1);
			boolean ready = ctx.computeOnClient(mc -> mc.hitResult != null
				&& mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY && mc.player.fallDistance > 3.0);
			if (ready) {
				at = "fallDistance=" + ctx.computeOnClient(mc -> String.format("%.2f", mc.player.fallDistance));
				if (useKey) ctx.getInput().pressKey(InputConstants.KEY_R);
				else ctx.getInput().pressKey(o -> o.keyAttack);
				break;
			}
		}
		ctx.waitTicks(30);
		conn.waitForClientboundPackets();
		sampling = false;
		String slots;
		synchronized (serverSlots) { slots = serverSlots.toString(); }
		if (slots.length() > 120) slots = slots.substring(0, 120) + "...";
		results.add(name + " | " + at + " | before " + before + " | after " + state(ctx, server, conn) + " | serverSlotPerTick " + slots);
	}

	private static String state(ClientGameTestContext ctx, TestServerContext server, TestServerConnection conn) {
		String client = ctx.computeOnClient(mc -> "clientSlot=" + mc.player.getInventory().getSelectedSlot()
			+ " hit=" + (mc.hitResult == null ? "null" : mc.hitResult.getType())
			+ String.format(" rot=%.1f/%.1f", mc.player.getYRot(), mc.player.getXRot()));
		String srv = server.computeOnServer(s -> {
			ServerPlayer p = conn.getServerPlayer();
			Cow cow = null;
			for (Entity e : s.overworld().getAllEntities()) if (e instanceof Cow c && c.entityTags().contains("t")) cow = c;
			StringBuilder sb = new StringBuilder("serverSlot=" + p.getInventory().getSelectedSlot());
			sb.append(String.format(" charge=%.2f", p.getAttackStrengthScale(0)));
			sb.append(" cowHp=").append(cow == null ? "none" : String.format("%.2f", cow.getHealth()));
			sb.append(" cowFire=").append(cow == null ? "-" : cow.getRemainingFireTicks());
			for (int i : new int[] {0, 3, 6}) {
				ItemStack st = p.getInventory().getItem(i);
				sb.append(" s").append(i + 1).append('=').append(st.getItem().toString().replace("minecraft:", "")).append(":dmg").append(st.getDamageValue());
			}
			return sb.toString();
		});
		return client + " " + srv;
	}
}
