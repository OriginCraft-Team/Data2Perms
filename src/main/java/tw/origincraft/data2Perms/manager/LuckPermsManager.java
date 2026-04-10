package tw.origincraft.data2Perms.manager;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;

import java.util.Map;
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
     * Loads the user by UUID, optionally removes existing nodes according to the delete scope,
     * then adds the new node {@code permission.value} with contexts and saves.
     *
     * @param uuid       the player's UUID
     * @param permission the permission prefix (e.g. {@code "residence.max"})
     * @param value      the number to append, producing a node like {@code "residence.max.36"}
     * @return a {@link CompletableFuture} resolving to {@code true} on success, {@code false} on failure
     */
    public CompletableFuture<Boolean> applyPermission(
            UUID uuid,
            String permission,
            int value,
            Map<String, String> contexts,
            DataManager.DeleteScope deleteScope
    ) {
        return luckPerms.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null) {
                logger.warning("Failed to load user " + uuid + ", skipping.");
                return false;
            }

            String prefix = permission + ".";
            switch (deleteScope) {
                case NONE -> {
                }
                case ALL_PREFIX -> user.data().clear(node -> node.getKey().startsWith(prefix));
                case SAME_CONTEXTS -> user.data().clear(node -> node.getKey().startsWith(prefix) && matchesContexts(node, contexts));
            }

            ImmutableContextSet.Builder contextBuilder = ImmutableContextSet.builder();
            for (Map.Entry<String, String> entry : contexts.entrySet()) {
                contextBuilder.add(entry.getKey(), entry.getValue());
            }

            user.data().add(Node.builder(permission + "." + value).withContext(contextBuilder.build()).build());
            luckPerms.getUserManager().saveUser(user);
            return true;
        }).exceptionally(ex -> {
            logger.severe("Error processing user " + uuid + ": " + ex.getMessage());
            return false;
        });
    }

    private boolean matchesContexts(Node node, Map<String, String> contexts) {
        if (contexts.isEmpty()) {
            return node.getContexts().isEmpty();
        }

        if (node.getContexts().size() != contexts.size()) {
            return false;
        }

        for (Map.Entry<String, String> entry : contexts.entrySet()) {
            if (!node.getContexts().contains(entry.getKey(), entry.getValue())) {
                return false;
            }
        }

        return true;
    }
}
