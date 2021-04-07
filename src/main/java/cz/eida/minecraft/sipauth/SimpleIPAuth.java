package cz.eida.minecraft.sipauth;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class SimpleIPAuth extends JavaPlugin implements Listener {

    File messagesFile = new File(this.getDataFolder(), "messages.yml");
    File playerLoginFile;

    FileConfiguration playerLogins;
    FileConfiguration messages;

    Logger logger;

    @Override
    public void onEnable() {

        if (!this.getDataFolder().exists()) {
            this.initialize();
        }

        logger = this.getLogger();
        this.getServer().getPluginManager().registerEvents(this, this);

        reloadConfig();
        reloadPlayerLogins();

        logger.info("SimpleIPAuth plugin enabled.");
    }

    @Override
    public void onDisable() {
        logger.info("SimpleIPAuth plugin disabled.");
    }

    @Override
    public void reloadConfig() {
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
        this.playerLoginFile = new File(this.getDataFolder(), "players.yml");
        reloadPlayerLogins();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!command.getName().equalsIgnoreCase("sipauth")) {
            return false;
        }

        if(args.length < 1) {
            return false;
        }

        if (sender instanceof Player) {
            sender = (Player) sender;
        }

        // reload - OP only
        if(args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("sipauth.reload")) {

                reloadConfig();

                logger.info("Configuration and player networks reloaded.");

                if(sender instanceof Player) {
                    sender.sendMessage(ChatColor.GREEN + messages.getString("reloaded"));
                }
            } else {
                if(sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + messages.getString("notpermitted"));
                }
                logger.info("");
            }
        }

        // list
        if(args[0].equalsIgnoreCase("list")) {

            Player infoPlayer = (Player) sender;

            if (args.length > 1 && sender.hasPermission("sipauth.manage")) {
                infoPlayer = getServer().getPlayer(args[1]);

                if (infoPlayer == null) {
                    sender.sendMessage(ChatColor.RED + messages.getString("notfound"));
                    return false;
                }

            } else {
                if(sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + messages.getString("notpermitted"));
                }
                return false;
            }

            StringBuilder netBuilder = new StringBuilder();
            String[] playerNetworks = getPlayerNetworks(infoPlayer);

            int i;
            for (i = 0; i < playerNetworks.length; i++) {
                netBuilder.append(playerNetworks[i]);

                if (i < (playerNetworks.length - 1)) {
                    netBuilder.append(", ");
                }
            }

            sender.sendMessage(messages.getString("networks") + " pro " + infoPlayer.getName() + ": " + netBuilder.toString());

            return true;
        }

        // add/remove
        if(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {

            Player managePlayer = (Player) sender;

            if (args.length > 2 && sender.hasPermission("sipauth.manage")) {
                managePlayer = getServer().getPlayer(args[2]);

                if (managePlayer == null) {
                    sender.sendMessage(ChatColor.RED + messages.getString("notfound"));
                    return false;
                }

            } else {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + messages.getString("notpermitted"));
                }
                return false;
            }

            // address format invalid
            if (!IPv4Matcher.isValid(args[1])) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + messages.getString("invalid"));
                }
                return false;
            }

            // add
            if (args[0].equalsIgnoreCase("add")) {
                addPlayerNetwork(managePlayer, args[1]);
                return true;
            }

            // remove
            if (args[0].equalsIgnoreCase("remove")) {
                removePlayerNetwork(managePlayer, args[1]);
                return true;
            }

            return false;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void checkPlayerIP(PlayerLoginEvent event) {

        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();
        String loginIP = event.getAddress().getHostAddress();

        if (player.hasPermission("sipauth.bypass")) {
            logger.info("Allowing " + playerName + " (has bypass permission).");
            return;
        }

        String[] playerIPdata = new String[]{};
        try {
            playerIPdata = getPlayerNetworks(player);
        } catch (Exception ex) {
            createPlayerEntry(player, new String[]{loginIP + "/32"});
            logger.info("Allowing new player " + playerName + " (" + loginIP + "/32)." );
            return;
        }

        IPv4Matcher ipmatcher = new IPv4Matcher(loginIP);

        if (ipmatcher.matchAny(playerIPdata)) {
            logger.info("Allowing " + playerName + " (" + loginIP + "/32)." );
        } else {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, messages.getString("notallowed"));
        }
    }

    /**
     * Create new player entry.
     *
     * @param player Player
     * @param newtorks Initial networks
     */
    private void createPlayerEntry(Player player, String[] newtorks) {
        playerLogins.set(player.getUniqueId() + ".name", player.getName());
        setPlayerNetworks(player, newtorks);
    }

    /**
     * Read player saved networks.
     *
     * @param player Player
     * @return allowed networks
     */
    private String[] getPlayerNetworks(Player player) {

        List<String> list = playerLogins.getStringList(player.getUniqueId() + ".networks");

        String[] result = new String[list.size()];
        int i;
        for (i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }

        return result;
    }

    private void setPlayerNetworks(Player player, String[] networks) {
        playerLogins.set(player.getUniqueId() + ".networks", networks);
        savePlayerLogins();
    }

    private void addPlayerNetwork(Player player, String network) {
        String[] currentNetworks = getPlayerNetworks(player);
        String[] newNetworks = new String[currentNetworks.length + 1];

        int i;
        for (i = 0; i < currentNetworks.length; i++) {
            newNetworks[i] = currentNetworks[i];
        }
        currentNetworks[i + 1] = network;

        setPlayerNetworks(player, newNetworks);
    }

    private void removePlayerNetwork(Player player, String network) {
        String[] currentNetworks = getPlayerNetworks(player);
        ArrayList<String> newNetworks = new ArrayList<>();
        for (String net : currentNetworks) {
            if (net.equals(network)) {
                continue;
            }
            newNetworks.add(net);
        }

        String[] reducedNetworks = new String[newNetworks.size()];
        int i;
        for (i = 0; i < reducedNetworks.length; i++) {
            reducedNetworks[i] = newNetworks.get(i);
        }

        setPlayerNetworks(player, reducedNetworks);
    }

    /**
     * Save player logins.
     */
    private void savePlayerLogins() {
        try {
            playerLogins.save(playerLoginFile);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to save player configuration.");
        }
    }

    /**
     * Load player logins from file.
     */
    private void reloadPlayerLogins() {
        playerLogins = YamlConfiguration.loadConfiguration(playerLoginFile);
    }

    private void initialize() {
        this.getDataFolder().mkdirs();
        this.saveResource(messagesFile.getName(), true);
    }

    public static void main(String[] args) {
        System.out.println("SimpleIPAuth: This is a Spigot plugin. Copy this jar file to plugins directory and reload your server.");
    }

}
