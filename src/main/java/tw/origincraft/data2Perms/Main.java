package tw.origincraft.data2Perms;

import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import tw.origincraft.data2Perms.manager.CommandManager;
import tw.origincraft.data2Perms.manager.DataManager;
import tw.origincraft.data2Perms.manager.LuckPermsManager;

public final class Main extends JavaPlugin {

    /**
     * Called when the plugin is enabled. Saves the default config, obtains the LuckPerms
     * API provider, and registers all managers and commands. Disables the plugin if
     * LuckPerms is not present.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();

        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            getLogger().severe("LuckPerms not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        LuckPerms luckPerms = provider.getProvider();
        DataManager dataManager = new DataManager(this);
        LuckPermsManager luckPermsManager = new LuckPermsManager(luckPerms, getLogger());
        CommandManager commandManager = new CommandManager(this, dataManager, luckPermsManager);

        getCommand("data2perms").setExecutor(commandManager);
        getLogger().info("Data2Perms enabled.");
    }

    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        getLogger().info("Data2Perms disabled.");
    }
}
