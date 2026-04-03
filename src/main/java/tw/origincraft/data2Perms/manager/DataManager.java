package tw.origincraft.data2Perms.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import tw.origincraft.data2Perms.Main;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class DataManager {

    /**
     * Holds the result of a single mapping entry from config.yml.
     *
     * @param permission the LuckPerms permission prefix (e.g. {@code "residence.max"})
     * @param data       a map of player UUIDs to their corresponding integer values
     */
    public record MappingEntry(String permission, Map<UUID, Integer> data) {}

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

            results.add(new MappingEntry(permission, data));
            log.info("Loaded " + data.size() + " entries from section '" + section + "' in " + filePath);
        }

        return results;
    }
}
