package tw.origincraft.data2Perms.manager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import tw.origincraft.data2Perms.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandManager implements CommandExecutor {

    private final Main plugin;
    private final DataManager dataManager;
    private final LuckPermsManager luckPermsManager;

    /**
     * @param plugin           the main plugin instance
     * @param dataManager      used to load mapping entries from config and YAML files
     * @param luckPermsManager used to apply permission nodes to players
     */
    public CommandManager(Main plugin, DataManager dataManager, LuckPermsManager luckPermsManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.luckPermsManager = luckPermsManager;
    }

    /**
     * Handles the {@code /data2perms} command. Requires the {@code data2perms.admin} permission.
     * <p>
     * Subcommands:
     * <ul>
     *   <li>{@code sync}   — loads all mappings and applies permission nodes to all listed players</li>
     *   <li>{@code reload} — reloads {@code config.yml} from disk</li>
     * </ul>
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("data2perms.admin")) {
            sender.sendMessage("[Data2Perms] You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("[Data2Perms] Usage: /data2perms <sync|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sync" -> handleSync(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage("[Data2Perms] Unknown subcommand. Usage: /data2perms <sync|reload>");
        }

        return true;
    }

    /**
     * Reloads {@code config.yml} from disk and notifies the sender.
     *
     * @param sender the command sender to notify
     */
    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage("[Data2Perms] Config reloaded.");
    }

    /**
     * Loads all mappings from config and asynchronously applies LuckPerms permission nodes
     * for every UUID found. Reports total success and failure counts to the sender once all
     * futures complete.
     *
     * @param sender the command sender to receive progress and result messages
     */
    private void handleSync(CommandSender sender) {
        sender.sendMessage("[Data2Perms] Starting sync...");

        List<DataManager.MappingEntry> entries = dataManager.loadMappings();
        if (entries.isEmpty()) {
            sender.sendMessage("[Data2Perms] No mappings found or all files missing.");
            return;
        }

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (DataManager.MappingEntry entry : entries) {
            for (Map.Entry<UUID, Integer> e : entry.data().entrySet()) {
                CompletableFuture<Void> future = luckPermsManager
                        .applyPermission(e.getKey(), entry.permission(), e.getValue(), entry.contexts(), entry.deleteScope())
                        .thenAccept(ok -> {
                            if (ok) success.incrementAndGet();
                            else failed.incrementAndGet();
                        });
                futures.add(future);
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() ->
                sender.sendMessage("[Data2Perms] Sync complete. Success: " + success.get() + ", Failed: " + failed.get())
        );
    }
}
