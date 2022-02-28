package cz.eida.minecraft.sipauth;

import cz.eida.minecraft.sipauth.ipmatcher.IPMatcher;
import cz.eida.minecraft.sipauth.utils.UUIDTools;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
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

        // reload
        if(args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("sipauth.reload")) {

                reloadConfig();

                logger.info("Messages and player networks reloaded.");

                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.GREEN + messages.getString("reloaded"));
                }

            } else {

                logger.info("Cannot reload: insufficient permissions.");

                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + messages.getString("notpermitted"));
                }
            }

            return true;
        }

        // list
        if(args[0].equalsIgnoreCase("list")) {

            if (args.length == 1 && !(sender instanceof Player)) {
                logger.info("Console usage: /sipauth list <player>");
                return true;
            }

            Player infoPlayer;
            UUID infoPlayerUUID;

            if (args.length > 1) {

                if (sender.hasPermission("sipauth.manage")) {

                    // online player
                    infoPlayer = getServer().getPlayer(args[1]);

                    // offline player
                    if (infoPlayer == null) {
                        UUID offlinePlayerUUID;

                        if (this.getServer().getOnlineMode()) {
                            offlinePlayerUUID = UUIDTools.getOnlineModePlayerUUID(args[1]);
                        } else {
                            offlinePlayerUUID = UUIDTools.getOfflineModePlayerUUID(args[1]);
                        }

                        infoPlayerUUID = this.getServer().getOfflinePlayer(offlinePlayerUUID).getUniqueId();
                    } else {
                        infoPlayerUUID = infoPlayer.getUniqueId();
                    }

                } else {
                    if (sender instanceof Player) {
                        sender.sendMessage(ChatColor.RED + messages.getString("notpermitted"));
                    }
                    return true;
                }
            } else {
                // self
                infoPlayer = (Player) sender;
                infoPlayerUUID = ((Player) sender).getUniqueId();
            }

            StringBuilder netBuilder = new StringBuilder();
            Iterator<String> networkIterator = getPlayerNetworks(infoPlayerUUID).iterator();
            while (networkIterator.hasNext()) {
                netBuilder.append(networkIterator.next());

                if (networkIterator.hasNext()) {
                    netBuilder.append(", ");
                }
            }

            if (netBuilder.length() == 0) {

                if (sender instanceof Player) {
                    sender.sendMessage(messages.getString("nonets") + " " + ((infoPlayer != null) ? infoPlayer.getName() : args[1]) + ".");
                } else {
                    logger.info("No networks defined for player " + ((infoPlayer != null) ? infoPlayer.getName() : args[1]) + ".");
                }

            } else {

                if (sender instanceof Player) {
                    sender.sendMessage(messages.getString("networks") + " " + ((infoPlayer != null) ? infoPlayer.getName() : args[1]) + ": " + netBuilder.toString());
                } else {
                    logger.info("Allowed networks for " + ((infoPlayer != null) ? infoPlayer.getName() : args[1]) + ": " + netBuilder.toString());
                }
            }

            return true;
        }

        // add/remove
        if(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {

            if (args.length < 2) {
                logger.info("Console usage: /sipauth (add | remove) <network> <player>");
                return false;
            }

            if (args.length == 2 && !(sender instanceof Player)) {
                logger.info("Console usage: /sipauth (add | remove) <network> <player>");
                return true;
            }

            Player managePlayer;
            UUID managePlayerUUID;

            if (args.length == 3) {

                if (sender.hasPermission("sipauth.manage")) {

                    // online player
                    managePlayer = getServer().getPlayer(args[2]);

                    // offline player
                    if (managePlayer == null) {
                        UUID offlinePlayerUUID;

                        if (this.getServer().getOnlineMode()) {
                            offlinePlayerUUID = UUIDTools.getOnlineModePlayerUUID(args[2]);
                        } else {
                            offlinePlayerUUID = UUIDTools.getOfflineModePlayerUUID(args[2]);
                        }

                        managePlayerUUID = this.getServer().getOfflinePlayer(offlinePlayerUUID).getUniqueId();
                    } else {
                        managePlayerUUID = managePlayer.getUniqueId();
                    }

                } else {
                    if (sender instanceof Player) {
                        sender.sendMessage(ChatColor.RED + messages.getString("notpermitted"));
                    }
                    return true;
                }

            } else {
                // self
                managePlayer = (Player) sender;
                managePlayerUUID = ((Player) sender).getUniqueId();
            }

            // address format invalid
            if (!IPMatcher.isValid(args[1])) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + messages.getString("invalid"));
                } else {
                    logger.info("Address format invalid.");
                }
                return true;
            }

            // add
            if (args[0].equalsIgnoreCase("add")) {
                if (managePlayer == null) {
                    addOfflinePlayerNetwork(args[2], args[1]);
                } else {
                    addPlayerNetwork(managePlayer, args[1]);
                }

                if (sender instanceof Player) {
                    sender.sendMessage(messages.getString("added") + " " + args[1] + " " + messages.getString("added_s") + " " + ((managePlayer != null) ? managePlayer.getName() : args[2]) + ".");
                } else {
                    logger.info("Added network " + args[1] + " to player " + ((managePlayer != null) ? managePlayer.getName() : args[2]) + " list.");
                }

                return true;
            }

            // remove
            if (args[0].equalsIgnoreCase("remove")) {
                if (managePlayer == null) {
                    removeOfflinePlayerNetwork(args[2], args[1]);
                } else {
                    removePlayerNetwork(managePlayer, args[1]);
                }

                if (sender instanceof Player) {
                    sender.sendMessage(messages.getString("removed") + " " + args[1] + " " + messages.getString("removed_s") + " " + ((managePlayer != null) ? managePlayer.getName() : args[2]) + ".");
                } else {
                    logger.info("Removed network " + args[1] + " from player " + ((managePlayer != null) ? managePlayer.getName() : args[2]) + " list.");
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Login event.
     *
     * @param event login event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void authNetwork(PlayerLoginEvent event) {

        Player player = event.getPlayer();
        String playerName = player.getName();
        String loginIP = event.getAddress().getHostAddress();

        if (player.hasPermission("sipauth.bypass")) {
            logger.info("Allowing " + playerName + " (has bypass permission).");
            return;
        }

        List<String> playerNetworks = getPlayerNetworks(player);

        IPMatcher ipMatcher = new IPMatcher(loginIP);

        if (playerNetworks.size() > 0) {
            playerNetworks = getPlayerNetworks(player);
        } else {
            playerNetworks = new ArrayList<>();
            playerNetworks.add(loginIP + "/" + ipMatcher.getSingleHostMask());
            createPlayerEntry(player, playerNetworks);
            logger.info("Allowing new player " + playerName + " (" + loginIP + "/" + ipMatcher.getSingleHostMask() + ").");
            return;
        }

        if (ipMatcher.matchAny(playerNetworks)) {
            logger.info("Allowing " + playerName + " (" + loginIP + "/" + ipMatcher.getSingleHostMask() + ").");
        } else {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, messages.getString("notallowed"));
        }
    }

    /**
     * Create new player entry.
     *
     * @param player player
     * @param newtorks initial networks
     */
    private void createPlayerEntry(Player player, List<String> newtorks) {
        playerLogins.set(player.getUniqueId() + ".name", player.getName());
        setPlayerNetworks(player, newtorks);
    }

    /**
     * Create new offline player entry.
     * Case-sensitive.
     *
     * @param offlineNickname offline player nickname
     * @param networks initial networks
     */
    private void createOfflinePlayerEntry(String offlineNickname, List<String> networks) {
        playerLogins.set(((this.getServer().getOnlineMode()) ? UUIDTools.getOnlineModePlayerUUID(offlineNickname) : UUIDTools.getOfflineModePlayerUUID(offlineNickname)).toString() + ".name", offlineNickname);
        setOfflinePlayerNetworks(offlineNickname, networks);
    }

    /**
     * Read player allowed networks.
     *
     * @param player player
     * @return allowed networks
     */
    private List<String> getPlayerNetworks(Player player) {
        return playerLogins.getStringList(player.getUniqueId() + ".networks");
    }

    /**
     * Read offline player networks.
     *
     * @param offlinePlayer
     * @return defined networks
     */
    private List<String> getPlayerNetworks(UUID offlinePlayer) {

        if (!playerLogins.contains(offlinePlayer.toString())) {
            return new ArrayList<>();
        }

        if (playerLogins.getString(offlinePlayer.toString() + ".name").length() != 0) {
            return playerLogins.getStringList(offlinePlayer.toString() + ".networks");
        }

        return null;
    }

    /**
     * Set allowed networks for player.
     *
     * @param player player instance
     * @param networks list of allowed networks
     */
    private void setPlayerNetworks(Player player, List<String> networks) {
        playerLogins.set(player.getUniqueId() + ".networks", networks.toArray());
        savePlayerLogins();
        reloadPlayerLogins();
    }

    /**
     * Set allowed networks for offline player.
     * Case-sensitive.
     *
     * @param offlineNickname offline player nickname
     * @param networks list of allowed networks
     */
    private void setOfflinePlayerNetworks(String offlineNickname, List<String> networks) {
        playerLogins.set(((this.getServer().getOnlineMode()) ? UUIDTools.getOnlineModePlayerUUID(offlineNickname) : UUIDTools.getOfflineModePlayerUUID(offlineNickname)).toString() + ".networks", networks.toArray());
        savePlayerLogins();
        reloadPlayerLogins();
    }

    /**
     * Add new network to allowed.
     *
     * @param player player instance
     * @param network network to add
     */
    private void addPlayerNetwork(Player player, String network) {
        ArrayList<String> networks = new ArrayList<>(getPlayerNetworks(player));
        networks.add(network);
        setPlayerNetworks(player, networks);
    }

    /**
     * Add new network to allowed for offline player.
     * Case-sensitive.
     *
     * @param offlineNickname offline player nickname
     * @param network network to add
     */
    private void addOfflinePlayerNetwork(String offlineNickname, String network) {
        UUID offlinePlayerUUID = (this.getServer().getOnlineMode()) ? UUIDTools.getOnlineModePlayerUUID(offlineNickname) : UUIDTools.getOfflineModePlayerUUID(offlineNickname);
        ArrayList<String> networks = new ArrayList<>(getPlayerNetworks(offlinePlayerUUID));

        // create initial network
        if (networks.size() == 0) {
            ArrayList<String> newNetworks = new ArrayList<>();
            newNetworks.add(network);
            this.createOfflinePlayerEntry(offlineNickname, newNetworks);

            return;
        }

        // add to existing
        networks.add(network);
        setOfflinePlayerNetworks(offlineNickname, networks);
    }

    /**
     * Remove network from allowed.
     *
     * @param player player instance
     * @param network network to remove
     */
    private void removePlayerNetwork(Player player, String network) {
        ArrayList<String> networks = new ArrayList<>(getPlayerNetworks(player));

        if (networks.contains(network)) {
            networks.remove(network);
            setPlayerNetworks(player, networks);
        }
    }

    /**
     * Remove offline player network from allowed ones.
     * Case-sensitive.
     *
     * @param offlineNickname offline player nickname
     * @param network network to remove
     */
    private void removeOfflinePlayerNetwork(String offlineNickname, String network) {
        UUID offlinePlayerUUID = (this.getServer().getOnlineMode()) ? UUIDTools.getOnlineModePlayerUUID(offlineNickname) : UUIDTools.getOfflineModePlayerUUID(offlineNickname);
        ArrayList<String> networks = new ArrayList<>(getPlayerNetworks(offlinePlayerUUID));

        // no networks
        if (networks.size() == 0) {
            return;
        }

        // remove network
        if (networks.contains(network)) {
            networks.remove(network);
            setOfflinePlayerNetworks(offlineNickname, networks);
        }
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

    /**
     * Copy resources.
     */
    private void initialize() {
        this.getDataFolder().mkdirs();
        this.saveResource(messagesFile.getName(), true);
    }

    public static void main(String[] args) {
        System.out.println("SimpleIPAuth: This is a Spigot plugin. Copy this jar file to plugins directory and reload your server.");
    }

}
