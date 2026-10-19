package pro.bhushan.attributeswap.config;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stored in config/attributeswap.json. */
public final class AttributeSwapConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("attributeswap");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static AttributeSwapConfig instance;

	/** Master switch. Off means the swap keys do nothing. */
	public boolean enabled = true;

	/** Switch back to the slot you were on in the same tick, so its attack cooldown is kept. */
	public boolean returnToSlot = true;

	public static AttributeSwapConfig get() {
		if (instance == null) instance = load();
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("attributeswap.json");
	}

	private static AttributeSwapConfig load() {
		Path path = path();
		AttributeSwapConfig config = null;
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				config = GSON.fromJson(reader, AttributeSwapConfig.class);
			} catch (Exception e) {
				LOGGER.warn("Could not read {}, using defaults", path, e);
			}
		}
		return config == null ? new AttributeSwapConfig() : config;
	}

	public void save() {
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (Exception e) {
			LOGGER.warn("Could not write {}", path, e);
		}
	}
}
