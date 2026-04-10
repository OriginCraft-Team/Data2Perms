package tw.origincraft.data2Perms.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import tw.origincraft.data2Perms.Main;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class DataManager {

    public enum DeleteScope {
        NONE,
        SAME_CONTEXTS,
        ALL_PREFIX
    }

    /**
     * Holds the result of a single mapping entry from config.yml.
     *
     * @param permission the LuckPerms permission prefix (e.g. {@code "residence.max"})
     * @param contexts   the LuckPerms contexts to apply to generated nodes
     * @param deleteScope controls which existing nodes are cleared before writing
     * @param data       a map of player UUIDs to their corresponding integer values
     */
    public record MappingEntry(String permission, Map<String, String> contexts, DeleteScope deleteScope, Map<UUID, Integer> data) {}

    private final Main plugin;

    /**
     * @param plugin the main plugin instance used to access config and logger
     */
    public DataManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Reads all mappings defined in {@code config.yml} and parses each referenced YAML file.
     * <p>
     * For each mapping, the specified section is read as a {@code UUID -> Integer} map.
     * Entries are skipped with a warning logged when:
     * <ul>
     *   <li>The mapping is missing {@code file}, {@code section}, or {@code permission} keys</li>
     *   <li>The referenced file does not exist</li>
     *   <li>The specified section is absent in the file</li>
     *   <li>A key is not a valid UUID</li>
     *   <li>A value is null, non-integer, or less than or equal to zero</li>
     * </ul>
     *
     * @return a list of successfully parsed {@link MappingEntry} objects
     */
    public List<MappingEntry> loadMappings() {
        Logger log = plugin.getLogger();
        List<Map<?, ?>> mappings = plugin.getConfig().getMapList("mappings");
        List<MappingEntry> results = new ArrayList<>();

        for (Map<?, ?> mapping : mappings) {
            String filePath = (String) mapping.get("file");
            String section = (String) mapping.get("section");
            String permission = (String) mapping.get("permission");
            Map<String, String> contexts = parseContexts(mapping.get("contexts"), log);
            DeleteScope deleteScope = parseDeleteScope(mapping.get("delete_scope"), log);

            if (filePath == null || section == null || permission == null) {
                log.warning("Skipping invalid mapping entry (missing file/section/permission): " + mapping);
                continue;
            }

            File file = new File(filePath);
            if (!file.exists()) {
                log.severe("Mapped file not found: " + file.getAbsolutePath());
                continue;
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection sec = yaml.getConfigurationSection(section);
            if (sec == null) {
                log.warning("Section '" + section + "' not found in file: " + filePath);
                continue;
            }

            Map<UUID, Integer> data = new HashMap<>();
            for (String key : sec.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    log.warning("Invalid UUID '" + key + "' in section '" + section + "', skipping.");
                    continue;
                }

                Object rawValue = sec.get(key);
                if (rawValue == null) {
                    log.warning("UUID " + key + " has null value in section '" + section + "', skipping.");
                    continue;
                }

                if (!(rawValue instanceof Integer value)) {
                    log.warning("UUID " + key + " has non-integer value '" + rawValue + "' in section '" + section + "', skipping.");
                    continue;
                }

                if (value <= 0) {
                    log.warning("UUID " + key + " has invalid value " + value + " (must be > 0) in section '" + section + "', skipping.");
                    continue;
                }

                data.put(uuid, value);
            }

            results.add(new MappingEntry(permission, contexts, deleteScope, data));
            log.info("Loaded " + data.size() + " entries from section '" + section + "' in " + filePath);
        }

        return results;
    }

    private Map<String, String> parseContexts(Object rawContexts, Logger log) {
        if (rawContexts == null) {
            return Map.of();
        }
        if (!(rawContexts instanceof Map<?, ?> contextMap)) {
            log.warning("Invalid 'contexts' type (must be key/value object), skipping contexts.");
            return Map.of();
        }

        Map<String, String> contexts = new HashMap<>();
        for (Map.Entry<?, ?> entry : contextMap.entrySet()) {
            Object rawKey = entry.getKey();
            Object rawValue = entry.getValue();
            if (!(rawKey instanceof String key) || key.isBlank()) {
                log.warning("Invalid context key '" + rawKey + "', skipping.");
                continue;
            }
            if (!(rawValue instanceof String value) || value.isBlank()) {
                log.warning("Invalid context value for key '" + key + "': '" + rawValue + "', skipping.");
                continue;
            }
            contexts.put(key, value);
        }
        return contexts;
    }

    private DeleteScope parseDeleteScope(Object rawDeleteScope, Logger log) {
        if (rawDeleteScope == null) {
            return DeleteScope.NONE;
        }
        if (!(rawDeleteScope instanceof String value)) {
            log.warning("Invalid delete_scope type (must be string), defaulting to 'none'.");
            return DeleteScope.NONE;
        }

        return switch (value.toLowerCase(Locale.ROOT)) {
            case "none" -> DeleteScope.NONE;
            case "same_contexts" -> DeleteScope.SAME_CONTEXTS;
            case "all_prefix" -> DeleteScope.ALL_PREFIX;
            default -> {
                log.warning("Unknown delete_scope '" + value + "', defaulting to 'none'.");
                yield DeleteScope.NONE;
            }
        };
    }
}
