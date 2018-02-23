package co.aikar.advancementcommand;

import com.google.common.collect.ArrayListMultimap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class AdvancementCommand extends JavaPlugin implements Listener {

    private final ArrayListMultimap<String, CommandInfo> commandMap = ArrayListMultimap.create();
    private static final Pattern PLAYER = Pattern.compile("%player\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_DISPLAY = Pattern.compile("%playerdisplayname\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern UUID = Pattern.compile("%uuid\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADVANCEMENT = Pattern.compile("%advancement\b", Pattern.CASE_INSENSITIVE);
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
        this.reloadConfig();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.commandMap.clear();
        List<Map<?, ?>> commands = this.getConfig().getMapList("on_advancement");
        if (commands == null) {
            return;
        }
        int i = 0;
        for (Map<?, ?> info : commands) {
            i++;
            MemoryConfiguration config = new MemoryConfiguration();
            //noinspection unchecked
            config.addDefaults((Map<String, Object>) info);
            Configuration section = config.getDefaults();
            String advancement = section.getString("advancement");
            if (advancement == null) {
                this.getLogger().severe("Error, Advancement missing on entry " + i);
                continue;
            }
            advancement = advancement.toLowerCase();
            try {
                this.commandMap.put(advancement, new CommandInfo(section));
            } catch (Exception e) {
                this.getLogger().severe("Error in Command Configuration for advancement " + advancement + " at entry " + i + ": " + e.getMessage());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        String key = event.getAdvancement().getKey().toString();
        runCommands(event, key);
    }

    private void runCommands(PlayerAdvancementDoneEvent event, String key) {
        List<CommandInfo> commands = this.commandMap.get(key.toLowerCase());
        if (commands == null || commands.isEmpty()) {
            return;
        }
        for (CommandInfo info : commands) {
            if (info.alias != null) {
                doWithPermissions(info, event.getPlayer(), () -> runCommands(event, info.alias));
            } else {
                runCommand(event, key, info);
            }
        }
    }

    private void runCommand(PlayerAdvancementDoneEvent event, String key, CommandInfo info) {
        Player player = event.getPlayer();
        String name = player.getName();
        String displayName = player.getDisplayName();
        String command = PLAYER.matcher(info.command).replaceAll(name);
        command = PLAYER_DISPLAY.matcher(command).replaceAll(displayName);
        command = ADVANCEMENT.matcher(command).replaceAll(key);
        command = UUID.matcher(command).replaceAll(player.getUniqueId().toString());

        if (!(info.asPlayer)) {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            return;
        }

        String finalCommand = command;
        doWithPermissions(info, player, () -> Bukkit.getServer().dispatchCommand(player, finalCommand));
    }

    private void doWithPermissions(CommandInfo info, Player player, Runnable run) {
        PermissionAttachment permissions = info.permissions.isEmpty() ? null : player.addAttachment(this);
        boolean wasOp = player.isOp();
        try {
            if (permissions != null) {
                for (String permission : info.permissions) {
                    permissions.setPermission(permission, true);
                }
            }
            if (info.op && !wasOp) {
                player.setOp(true);
            }
            run.run();
        } finally {
            if (!wasOp && info.op) {
                player.setOp(false);
            }
            if (permissions != null) {
                permissions.remove();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.reloadConfig();
        sender.sendMessage("[AdvancementCommand] Config reloaded");
        return true;
    }

    private class CommandInfo {
        final boolean asPlayer;
        final String command;
        final String alias;
        final boolean op;
        final List<String> permissions = new ArrayList<>();
        CommandInfo(ConfigurationSection section) {
            this.asPlayer = !section.getString("run_as", "player").equals("console");
            this.command = section.getString("command");
            this.alias = section.getString("alias");
            boolean commandEmpty = this.command == null || this.command.isEmpty();
            if (this.alias != null && commandEmpty) {
                throw new Error("command can not be empty");
            }
            if (!commandEmpty && this.alias != null) {
                throw new Error("You can not have both a command and alias configuration");
            }
            this.op = section.getBoolean("op", false);
            String permission = section.getString("permission");
            if (permission != null) {
                this.permissions.add(permission);
            }
            List<String> permissions = section.getStringList("permissions");
            if (permissions != null) {
                this.permissions.addAll(permissions);
            }
        }
    }
}
