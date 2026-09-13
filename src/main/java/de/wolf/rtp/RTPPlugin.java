package de.wolf.rtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class RTPPlugin extends JavaPlugin implements Listener {

    private final HashMap<UUID, Long> cooldown = new HashMap<>();
    private final HashMap<UUID, Location> wartendeSpieler = new HashMap<>();

    private final Random random = new Random();

    private static final String MENU_NAME = "§8§lRTP";

    @Override
    public void onEnable() {

        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("WolfRTP gestartet!");
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!command.getName().equalsIgnoreCase("rtp")) {
            return true;
        }

        oeffneMenu(player);

        return true;
    }

    // =========================================
    // RTP MENÜ
    // =========================================

    private void oeffneMenu(Player player) {

        var inventory = Bukkit.createInventory(
                null,
                27,
                MENU_NAME
        );

        inventory.setItem(
                11,
                item(
                        Material.GRASS_BLOCK,
                        "§a§lOverworld"
                )
        );

        inventory.setItem(
                13,
                item(
                        Material.NETHERRACK,
                        "§c§lNether"
                )
        );

        inventory.setItem(
                15,
                item(
                        Material.END_STONE,
                        "§d§lEnd"
                )
        );

        player.openInventory(inventory);
    }

    private ItemStack item(
            Material material,
            String name) {

        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }

        return item;
    }

    // =========================================
    // MENÜ KLICK
    // =========================================

    @EventHandler
    public void menuKlick(InventoryClickEvent event) {

        if (!event.getView()
                .getTitle()
                .equals(MENU_NAME)) {

            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        int slot = event.getSlot();

        if (slot == 11) {

            player.closeInventory();

            starteRTP(
                    player,
                    World.Environment.NORMAL
            );

        } else if (slot == 13) {

            player.closeInventory();

            starteRTP(
                    player,
                    World.Environment.NETHER
            );

        } else if (slot == 15) {

            player.closeInventory();

            starteRTP(
                    player,
                    World.Environment.THE_END
            );
        }
    }

    // =========================================
    // RTP STARTEN
    // =========================================

    private void starteRTP(
            Player player,
            World.Environment environment) {

        UUID uuid = player.getUniqueId();

        long jetzt = System.currentTimeMillis();

        long cooldownZeit =
                getConfig().getLong(
                        "cooldown",
                        5
                );

        if (cooldown.containsKey(uuid)) {

            long letzteBenutzung =
                    cooldown.get(uuid);

            long vergangen =
                    (jetzt - letzteBenutzung) / 1000;

            long warten =
                    cooldownZeit - vergangen;

            if (warten > 0) {

                player.sendMessage(
                        "§cDu musst noch " +
                        warten +
                        " Sekunden warten."
                );

                return;
            }
        }

        World world = findeWelt(environment);

        if (world == null) {

            player.sendMessage(
                    "§cDiese Dimension wurde nicht gefunden."
            );

            return;
        }

        Location start =
                player.getLocation().clone();

        wartendeSpieler.put(uuid, start);

        player.sendMessage("");
        player.sendMessage(
                "§e§lRTP startet in 5 Sekunden..."
        );
        player.sendMessage(
                "§7Bewege dich nicht!"
        );

        new BukkitRunnable() {

            int sekunden = 5;

            @Override
            public void run() {

                if (!player.isOnline()) {

                    wartendeSpieler.remove(uuid);

                    cancel();

                    return;
                }

                Location aktuelle =
                        player.getLocation();

                Location ursprung =
                        wartendeSpieler.get(uuid);

                if (ursprung == null) {

                    cancel();

                    return;
                }

                if (istBewegt(
                        ursprung,
                        aktuelle)) {

                    player.sendMessage(
                            "§c§lRTP abgebrochen!"
                    );

                    player.sendMessage(
                            "§7Du hast dich bewegt."
                    );

                    wartendeSpieler.remove(uuid);

                    cancel();

                    return;
                }

                if (sekunden > 1) {

                    player.sendMessage(
                            "§eRTP in §f" +
                            sekunden +
                            " §eSekunden..."
                    );

                    sekunden--;

                    return;
                }

                wartendeSpieler.remove(uuid);

                Location ziel =
                        findeRtpOrt(world);

                if (ziel == null) {

                    player.sendMessage(
                            "§cKein sicherer Ort gefunden."
                    );

                    cancel();

                    return;
                }

                boolean erfolgreich =
                        player.teleport(ziel);

                if (erfolgreich) {

                    cooldown.put(
                            uuid,
                            System.currentTimeMillis()
                    );

                    player.sendMessage(
                            "§a§lRTP erfolgreich!"
                    );
                } else {

                    player.sendMessage(
                            "§cTeleport fehlgeschlagen."
                    );
                }

                cancel();
            }

        }.runTaskTimer(
                this,
                0L,
                20L
        );
    }

    // =========================================
    // BEWEGUNG PRÜFEN
    // =========================================

    private boolean istBewegt(
            Location start,
            Location aktuell) {

        if (!start.getWorld()
                .equals(aktuell.getWorld())) {

            return true;
        }

        double x =
                start.getX() - aktuell.getX();

        double y =
                start.getY() - aktuell.getY();

        double z =
                start.getZ() - aktuell.getZ();

        double entfernung =
                Math.sqrt(
                        x * x +
                        y * y +
                        z * z
                );

        return entfernung > 0.05;
    }

    // =========================================
    // WELT FINDEN
    // =========================================

    private World findeWelt(
            World.Environment environment) {

        for (World world : Bukkit.getWorlds()) {

            if (world.getEnvironment()
                    == environment) {

                return world;
            }
        }

        return null;
    }

    // =========================================
    // RTP POSITION
    // =========================================

    private Location findeRtpOrt(World world) {

        WorldBorder border =
                world.getWorldBorder();

        double radius =
                getConfig().getDouble(
                        "radius",
                        250000
                );

        int maxVersuche =
                getConfig().getInt(
                        "max-attempts",
                        50
                );

        for (int i = 0;
             i < maxVersuche;
             i++) {

            double winkel =
                    random.nextDouble()
                    * Math.PI * 2;

            double entfernung =
                    Math.sqrt(
                            random.nextDouble()
                    ) * radius;

            double x =
                    Math.cos(winkel)
                    * entfernung;

            double z =
                    Math.sin(winkel)
                    * entfernung;

            int blockX = (int) x;
            int blockZ = (int) z;

            int y =
                    world.getHighestBlockYAt(
                            blockX,
                            blockZ
                    );

            Location ziel =
                    new Location(
                            world,
                            x,
                            y + 1,
                            z
                    );

            if (!border.isInside(ziel)) {
                continue;
            }

            if (sicher(ziel)) {
                return ziel;
            }
        }

        return null;
    }

    // =========================================
    // SICHERHEIT
    // =========================================

    private boolean sicher(Location ziel) {

        Material boden =
                ziel.clone()
                        .subtract(0, 1, 0)
                        .getBlock()
                        .getType();

        Material fuesse =
                ziel.getBlock()
                        .getType();

        Material kopf =
                ziel.clone()
                        .add(0, 1, 0)
                        .getBlock()
                        .getType();

        if (!boden.isSolid()) {
            return false;
        }

        if (fuesse.isSolid()) {
            return false;
        }

        if (kopf.isSolid()) {
            return false;
        }

        if (boden == Material.LAVA ||
                boden == Material.WATER ||
                boden == Material.MAGMA_BLOCK ||
                boden == Material.CACTUS ||
                boden == Material.FIRE ||
                boden == Material.SOUL_FIRE ||
                boden == Material.POWDER_SNOW ||
                boden == Material.SWEET_BERRY_BUSH) {

            return false;
        }

        if (fuesse == Material.WATER ||
                fuesse == Material.LAVA) {

            return false;
        }

        if (kopf == Material.WATER ||
                kopf == Material.LAVA) {

            return false;
        }

        return true;
    }
}
