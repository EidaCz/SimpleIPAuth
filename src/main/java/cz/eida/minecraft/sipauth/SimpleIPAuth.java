package cz.eida.minecraft.sipauth;

import com.destroystokyo.paper.profile.PlayerProfile;
import cz.eida.minecraft.sipauth.ipmatcher.IPMatcher;
import cz.eida.minecraft.sipauth.utils.UUIDManager;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerConnection;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple IP Auth plugin - KonAuth project.
 *
 * @author EidaCz
 */
public class SimpleIPAuth extends JavaPlugin implements Listener {

    private static final List<String> BYPASS_NETWORKS = Arrays.asList("0.0.0.0/0", "::/0");

    File messagesFile = new File(this.getDataFolder(), "messages.yml");
    File playerLoginFile;

    FileConfiguration playerLogins;
    FileConfiguration messages;

    UUIDManager uuidManager;

    Logger logger;
    private final Object playerLoginLock = new Object();

    @Override
    public void onEnable() {
        logger = this.getLogger();

        if (!this.getDataFolder().exists()) {
            this.initialize();
        }

        try {
            this.uuidManager = new UUIDManager(this);
        } catch (IOException e) {
            logger.severe("Cannot create UUID cache.");
        }

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

        if (this.uuidManager != null) {
            try {
                this.uuidManager.reloadCache();
            } catch (IOException e) {
                logger.severe("Cannot load UUID cache file.");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!command.getName().equalsIgnoreCase("sipauth")) {
            return false;
        }

        if(args.length < 1) {
            return false;
        }

        // reload
        if(args[0].equalsIgnoreCase("reload")) {

            if (sender.hasPermission("sipauth.reload")) {

                reloadConfig();
                logger.info("Messages and player networks reloaded.");

                if (sender instanceof Player) {
                    sendMessage(sender, messages.getString("reloaded"), NamedTextColor.GREEN);
                }

            } else {

                logger.info("Cannot reload: insufficient permissions.");

                if (sender instanceof Player) {
                    sendMessage(sender, messages.getString("notpermitted"), NamedTextColor.RED);
                }
            }

            return true;
        }

        // clearcache
        if (args[0].equalsIgnoreCase("clearcache")) {
            if (sender.hasPermission("sipauth.reload")) {

                try {
                    uuidManager.clearCache();
                } catch (IOException e) {
                    logger.severe("Cannot clear cache: " + e.getLocalizedMessage());
                    return true;
                }
                logger.info("UUID cache cleared.");

                if (sender instanceof Player) {
                    sendMessage(sender, messages.getString("cachecleared"), NamedTextColor.GREEN);
                }

            } else {

                logger.info("Cannot clear cache: insufficient permissions.");

                if (sender instanceof Player) {
                    sendMessage(sender, messages.getString("notpermitted"), NamedTextColor.RED);
                }
            }

            return true;
        }

        // list
        if (args[0].equalsIgnoreCase("list")) {

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

                    if (infoPlayer != null) {
                        infoPlayerUUID = infoPlayer.getUniqueId();
                    } else {
                        // offline player
                        try {
                            infoPlayerUUID = uuidManager.getOfflinePlayerUUID(args[1]);
                        } catch (Exception e) {
                            logger.severe("Cannot fetch UUID: " + e.getLocalizedMessage());
                            return true;
                        }
                    }

                } else {
                    if (sender instanceof Player) {
                        sendMessage(sender, messages.getString("notpermitted"), NamedTextColor.RED);
                    }
                    return true;
                }
            } else {
                // sender self
                infoPlayer = (Player) sender;
                infoPlayerUUID = ((Player) sender).getUniqueId();
            }

            StringBuilder netBuilder = new StringBuilder();
            Iterator<String> networkIterator = Objects.requireNonNull(getPlayerNetworks(infoPlayerUUID)).iterator();
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
                    sender.sendMessage(messages.getString("networks") + " " + ((infoPlayer != null) ? infoPlayer.getName() : args[1]) + ": " + netBuilder);
                } else {
                    logger.info("Allowed networks for " + ((infoPlayer != null) ? infoPlayer.getName() : args[1]) + ": " + netBuilder);
                }
            }

            return true;
        }

        // bypass/unbypass
        if (args[0].equalsIgnoreCase("bypass") || args[0].equalsIgnoreCase("unbypass")) {

            if (args.length == 1 && !(sender instanceof Player)) {
                logger.info("Console usage: /sipauth (bypass | unbypass) <player>");
                return true;
            }

            if (args.length > 2) {
                logger.info("Usage: /sipauth (bypass | unbypass) [player]");
                return false;
            }

            Player managedPlayer;

            if (args.length == 2) {
                if (sender.hasPermission("sipauth.manage")) {
                    managedPlayer = getServer().getPlayer(args[1]);
                } else {
                    if (sender instanceof Player) {
                        sendMessage(sender, messages.getString("notpermitted"), NamedTextColor.RED);
                    }
                    return true;
                }
            } else {
                managedPlayer = (Player) sender;
            }

            boolean bypassEnabled = args[0].equalsIgnoreCase("bypass");
            String managedPlayerName = (managedPlayer != null) ? managedPlayer.getName() : args[1];

            try {
                if (managedPlayer == null) {
                    setOfflinePlayerBypass(managedPlayerName, bypassEnabled);
                } else {
                    setPlayerBypass(managedPlayer, bypassEnabled);
                }
            } catch (IOException e) {
                logger.severe("Cannot fetch UUID: " + e.getLocalizedMessage());
                return true;
            }

            String message = bypassEnabled ? messages.getString("bypassed") : messages.getString("unbypassed");
            if (sender instanceof Player) {
                sender.sendMessage(message + " " + managedPlayerName + ".");
            } else {
                logger.info(message + " " + managedPlayerName + ".");
            }

            return true;
        }

        // add/remove
        if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {

            if (args.length < 2) {
                logger.info("Console usage: /sipauth (add | remove) <network> <player>");
                return false;
            }

            if (args.length == 2 && !(sender instanceof Player)) {
                logger.info("Console usage: /sipauth (add | remove) <network> <player>");
                return true;
            }

            Player managedPlayer;

            // manage other player
            if (args.length == 3) {

                if (sender.hasPermission("sipauth.manage")) {
                    // online player
                    managedPlayer = getServer().getPlayer(args[2]);
                } else {
                    if (sender instanceof Player) {
                        sendMessage(sender, messages.getString("notpermitted"), NamedTextColor.RED);
                    }
                    return true;
                }

            } else {
                // self
                managedPlayer = (Player) sender;
            }

            // address format invalid
            if (!IPMatcher.isValid(args[1])) {
                if (sender instanceof Player) {
                    sendMessage(sender, messages.getString("invalid"), NamedTextColor.RED);
                } else {
                    logger.info("Address format invalid.");
                }
                return true;
            }

            // add
            if (args[0].equalsIgnoreCase("add")) {
                if (managedPlayer == null) {
                    try {
                        addOfflinePlayerNetwork(args[2], args[1]);
                    } catch (IOException e) {
                        logger.severe("Cannot fetch UUID: " + e.getLocalizedMessage());
                        return true;
                    }
                } else {
                    // self
                    addPlayerNetwork(managedPlayer, args[1]);
                }

                if (sender instanceof Player) {
                    sender.sendMessage(messages.getString("added") + " " + args[1] + " " + messages.getString("added_s") + " " + ((managedPlayer != null) ? managedPlayer.getName() : args[2]) + ".");
                } else {
                    logger.info("Added network " + args[1] + " to player " + ((managedPlayer != null) ? managedPlayer.getName() : args[2]) + " list.");
                }

                return true;
            }

            // remove
            if (args[0].equalsIgnoreCase("remove")) {
                if (managedPlayer == null) {
                    try {
                        removeOfflinePlayerNetwork(args[2], args[1]);
                    } catch (IOException e) {
                        logger.severe("Cannot fetch UUID: " + e.getLocalizedMessage());
                        return true;
                    }
                } else {
                    // self
                    removePlayerNetwork(managedPlayer, args[1]);
                }

                if (sender instanceof Player) {
                    sender.sendMessage(messages.getString("removed") + " " + args[1] + " " + messages.getString("removed_s") + " " + ((managedPlayer != null) ? managedPlayer.getName() : args[2]) + ".");
                } else {
                    logger.info("Removed network " + args[1] + " from player " + ((managedPlayer != null) ? managedPlayer.getName() : args[2]) + " list.");
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Login validation event.
     *
     * @param event connection validation event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void authNetwork(PlayerConnectionValidateLoginEvent event) {

        if (!event.isAllowed()) {
            return;
        }

        PlayerProfile profile = getProfile(event.getConnection());
        if (profile == null || profile.getId() == null || profile.getName() == null) {
            logger.warning("Denying login: missing authenticated player profile.");
            event.kickMessage(Component.text(Objects.requireNonNull(messages.getString("notallowed"))));
            return;
        }

        UUID playerUUID = profile.getId();
        String playerName = profile.getName();
        String loginIP = getClientAddress(event.getConnection());

        if (!IPMatcher.isValid(loginIP)) {
            logger.warning("Denying " + playerName + ": invalid client address " + loginIP + ".");
            event.kickMessage(Component.text(Objects.requireNonNull(messages.getString("notallowed"))));
            return;
        }

        IPMatcher ipMatcher = new IPMatcher(loginIP);

        synchronized (playerLoginLock) {
            if (!hasPlayerEntry(playerUUID)) {
                createPlayerEntry(playerUUID, playerName, new ArrayList<>(BYPASS_NETWORKS));
                logger.info("Allowing new player " + playerName + " (default IP bypass).");
                return;
            }

            List<String> playerNetworks = getPlayerNetworks(playerUUID);

            if (ipMatcher.matchAny(playerNetworks)) {
                logger.info("Allowing " + playerName + " (" + ipMatcher.getSanitizedAddress() + ").");
            } else {
                event.kickMessage(Component.text(Objects.requireNonNull(messages.getString("notallowed"))));
            }
        }
    }

    private PlayerProfile getProfile(PlayerConnection connection) {
        if (connection instanceof PlayerConfigurationConnection) {
            return ((PlayerConfigurationConnection) connection).getProfile();
        }

        if (connection instanceof PlayerLoginConnection) {
            PlayerLoginConnection loginConnection = (PlayerLoginConnection) connection;
            PlayerProfile profile = loginConnection.getAuthenticatedProfile();
            return (profile != null) ? profile : loginConnection.getUnsafeProfile();
        }

        return null;
    }

    private String getClientAddress(PlayerConnection connection) {
        InetSocketAddress clientAddress = connection.getClientAddress();
        InetAddress address = clientAddress.getAddress();
        return (address != null) ? address.getHostAddress() : clientAddress.getHostString();
    }

    private void sendMessage(CommandSender sender, String message, NamedTextColor color) {
        sender.sendMessage(Component.text(Objects.requireNonNull(message), color));
    }

    /**
     * Create new player entry.
     *
     * @param playerUUID player UUID
     * @param playerName player name
     * @param newtorks initial networks
     */
    private void createPlayerEntry(UUID playerUUID, String playerName, List<String> newtorks) {
        playerLogins.set(playerUUID + ".name", playerName);
        setPlayerNetworks(playerUUID, newtorks);
    }

    /**
     * Create new offline player entry.
     * Case-sensitive.
     *
     * @param offlineNickname offline player nickname
     * @param networks initial networks
     */
    private void createOfflinePlayerEntry(String offlineNickname, List<String> networks) throws IOException {
        playerLogins.set(uuidManager.getOfflinePlayerUUID(offlineNickname) + ".name", offlineNickname);
        setOfflinePlayerNetworks(offlineNickname, networks);
    }

    /**
     * Read player allowed networks.
     *
     * @param player player
     * @return allowed networks
     */
    private List<String> getPlayerNetworks(Player player) {
        return getPlayerNetworks(player.getUniqueId());
    }

    /**
     * Read offline player networks.
     *
     * @param offlinePlayer UUID of an offline player
     * @return defined networks
     */
    private List<String> getPlayerNetworks(UUID offlinePlayer) {

        if (!hasPlayerEntry(offlinePlayer)) {
            return new ArrayList<>();
        }

        return playerLogins.getStringList(offlinePlayer + ".networks");
    }

    /**
     * Check whether player has an explicit configuration entry.
     *
     * @param playerUUID player UUID
     * @return player entry exists
     */
    private boolean hasPlayerEntry(UUID playerUUID) {
        return playerLogins.contains(playerUUID.toString());
    }

    /**
     * Set allowed networks for player.
     *
     * @param player player instance
     * @param networks list of allowed networks
     */
    private void setPlayerNetworks(Player player, List<String> networks) {
        setPlayerNetworks(player.getUniqueId(), networks);
    }

    /**
     * Set allowed networks for player.
     *
     * @param playerUUID player UUID
     * @param networks list of allowed networks
     */
    private void setPlayerNetworks(UUID playerUUID, List<String> networks) {
        playerLogins.set(playerUUID + ".networks", networks);
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
    private void setOfflinePlayerNetworks(String offlineNickname, List<String> networks) throws IOException {
        playerLogins.set(uuidManager.getOfflinePlayerUUID(offlineNickname) + ".networks", networks);
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
        synchronized (playerLoginLock) {
            ArrayList<String> networks = new ArrayList<>(getPlayerNetworks(player));
            networks.add(network);
            setPlayerNetworks(player, networks);
        }
    }

    /**
     * Add new network to allowed for offline player.
     * Case-sensitive.
     *
     * @param offlineNickname offline player nickname
     * @param network network to add
     */
    private void addOfflinePlayerNetwork(String offlineNickname, String network) throws IOException {
        UUID offlinePlayerUUID = uuidManager.getOfflinePlayerUUID(offlineNickname);
        synchronized (playerLoginLock) {
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
    }

    /**
     * Enable or disable full IP bypass for online player.
     *
     * @param player player instance
     * @param enabled bypass enabled
     */
    private void setPlayerBypass(Player player, boolean enabled) {
        synchronized (playerLoginLock) {
            ArrayList<String> networks = new ArrayList<>(getPlayerNetworks(player));
            setBypassNetworks(networks, enabled);
            playerLogins.set(player.getUniqueId() + ".name", player.getName());
            setPlayerNetworks(player, networks);
        }
    }

    /**
     * Enable or disable full IP bypass for offline player.
     * Case-sensitive.
     *
     * @param offlineNickname offline player nickname
     * @param enabled bypass enabled
     */
    private void setOfflinePlayerBypass(String offlineNickname, boolean enabled) throws IOException {
        UUID offlinePlayerUUID = uuidManager.getOfflinePlayerUUID(offlineNickname);
        synchronized (playerLoginLock) {
            ArrayList<String> networks = new ArrayList<>(getPlayerNetworks(offlinePlayerUUID));

            if (!enabled && !playerLogins.contains(offlinePlayerUUID.toString())) {
                return;
            }

            setBypassNetworks(networks, enabled);
            playerLogins.set(offlinePlayerUUID + ".name", offlineNickname);
            setPlayerNetworks(offlinePlayerUUID, networks);
        }
    }

    private void setBypassNetworks(List<String> networks, boolean enabled) {
        if (enabled) {
            for (String bypassNetwork : BYPASS_NETWORKS) {
                if (!networks.contains(bypassNetwork)) {
                    networks.add(bypassNetwork);
                }
            }
        } else {
            networks.removeAll(BYPASS_NETWORKS);
        }
    }

    /**
     * Remove network from allowed.
     *
     * @param player player instance
     * @param network network to remove
     */
    private void removePlayerNetwork(Player player, String network) {
        synchronized (playerLoginLock) {
            ArrayList<String> networks = new ArrayList<>(getPlayerNetworks(player));

            if (networks.contains(network)) {
                networks.remove(network);
                setPlayerNetworks(player, networks);
            }
        }
    }

    /**
     * Remove offline player network from allowed ones.
     * Case-sensitive.
     *
     * @param offlineNickname offline player nickname
     * @param network network to remove
     */
    private void removeOfflinePlayerNetwork(String offlineNickname, String network) throws IOException {
        UUID offlinePlayerUUID = uuidManager.getOfflinePlayerUUID(offlineNickname);
        synchronized (playerLoginLock) {
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
    }

    /**
     * Save player logins.
     */
    private void savePlayerLogins() {
        try {
            playerLogins.save(playerLoginFile);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to save player configuration.", ex);
        }
    }

    /**
     * Load player logins from file.
     */
    private void reloadPlayerLogins() {
        synchronized (playerLoginLock) {
            playerLogins = YamlConfiguration.loadConfiguration(playerLoginFile);
        }
    }

    /**
     * Copy resources.
     */
    private void initialize() {
        this.getDataFolder().mkdirs();
        this.saveResource(messagesFile.getName(), true);
        this.saveResource("players.yml", false);
        this.saveResource("uuid-cache.yml", false);
    }

    public static void main(String[] args) {
        System.out.println("SimpleIPAuth: This is a Spigot plugin. Copy this jar file to plugins directory and reload your server.");
    }

}
