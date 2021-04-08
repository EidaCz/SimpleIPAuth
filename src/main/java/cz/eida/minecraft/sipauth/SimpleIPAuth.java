package cz.eida.minecraft.sipauth;

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

                //return true;
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

            if (args.length > 1) {

                if (sender.hasPermission("sipauth.manage")) {

                    // online player
                    infoPlayer = getServer().getPlayer(args[1]);

                    // TODO offline player

                    if (infoPlayer == null) {
                        if (sender instanceof Player) {
                            sender.sendMessage(ChatColor.RED + messages.getString("notfound"));
                        } else {
                            logger.info("Player " + args[1] + " not found.");
                        }
                        return true;
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
            }

            StringBuilder netBuilder = new StringBuilder();
            Iterator<String> networkIterator = getPlayerNetworks(infoPlayer).iterator();
            while (networkIterator.hasNext()) {
                netBuilder.append(networkIterator.next());

                if (networkIterator.hasNext()) {
                    netBuilder.append(", ");
                }
            }

            if (sender instanceof Player) {
                sender.sendMessage(messages.getString("networks") + " " + infoPlayer.getName() + ": " + netBuilder.toString());
            } else {
                logger.info("Allowed networks for " + infoPlayer.getName() + ": " + netBuilder.toString());
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

            if (args.length == 3) {

                if (sender.hasPermission("sipauth.manage")) {

                    // online player
                    managePlayer = getServer().getPlayer(args[2]);

                    // TODO offline player

                    if (managePlayer == null) {
                        if (sender instanceof Player) {
                            sender.sendMessage(ChatColor.RED + messages.getString("notfound"));
                        } else {
                            logger.info("Player " + args[2] + " not found.");
                        }
                        return true;
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
            }

            // address format invalid
            if (!IPv4Matcher.isValid(args[1])) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + messages.getString("invalid"));
                } else {
                    logger.info("Address format invalid.");
                }
                return true;
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

        logger.info("Data: " + playerNetworks.toString() + ", tj. size = " + playerNetworks.size());

        if (playerNetworks.size() > 0) {
            playerNetworks = getPlayerNetworks(player);
        } else {
            playerNetworks = new ArrayList<>();
            playerNetworks.add(loginIP + "/32");
            createPlayerEntry(player, playerNetworks);
            logger.info("Allowing new player " + playerName + " (" + loginIP + "/32).");
            return;
        }

        IPv4Matcher ipMatcher = new IPv4Matcher(loginIP);

        if (ipMatcher.matchAny(playerNetworks)) {
            logger.info("Allowing " + playerName + " (" + loginIP + "/32).");
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
     * Read player allowed networks.
     *
     * @param player player
     * @return allowed networks
     */
    private List<String> getPlayerNetworks(Player player) {
        return playerLogins.getStringList(player.getUniqueId() + ".networks");
    }

    /**
     * TODO offline player by UUID
     *
     * @param offlinePlayer
     * @return
     */
    private List<String> getPlayerNetworks(UUID offlinePlayer) {
        if (playerLogins.getString(offlinePlayer.toString()+ ".name").length() != 0) {
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
