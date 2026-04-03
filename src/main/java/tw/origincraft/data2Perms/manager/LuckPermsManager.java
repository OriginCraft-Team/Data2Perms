package tw.origincraft.data2Perms.manager;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class LuckPermsManager {

    private final LuckPerms luckPerms;
    private final Logger logger;

    /**
     * @param luckPerms the LuckPerms API instance
     * @param logger    the plugin logger for warnings and errors
     */
    public LuckPermsManager(LuckPerms luckPerms, Logger logger) {
        this.luckPerms = luckPerms;
        this.logger = logger;
    }

    /**
     * Asynchronously applies a permission node to a player in LuckPerms.
     * <p>
     * Loads the user by UUID, removes all existing nodes whose key starts with
     * {@code permission + "."}, then adds the new node {@code permission.value} and saves.
     *
     * @param uuid       the player's UUID
     * @param permission the permission prefix (e.g. {@code "residence.max"})
     * @param value      the number to append, producing a node like {@code "residence.max.36"}
     * @return a {@link CompletableFuture} resolving to {@code true} on success, {@code false} on failure
     */
    public CompletableFuture<Boolean> applyPermission(UUID uuid, String permission, int value) {
        return luckPerms.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null) {
                logger.warning("Failed to load user " + uuid + ", skipping.");
                return false;
            }

            String prefix = permission + ".";
            user.data().clear(node -> node.getKey().startsWith(prefix));
            user.data().add(Node.builder(permission + "." + value).build());
            luckPerms.getUserManager().saveUser(user);
            return true;
        }).exceptionally(ex -> {
            logger.severe("Error processing user " + uuid + ": " + ex.getMessage());
            return false;
        });
    }
}
