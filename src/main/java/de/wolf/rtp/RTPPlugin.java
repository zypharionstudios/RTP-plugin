package de.wolf.rtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class RTPPlugin extends JavaPlugin {

    private HashMap<UUID, Long> cooldown = new HashMap<>();
    private Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("RTP gestartet");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;

        if (!cmd.getName().equalsIgnoreCase("rtp")) {
            return true;
        }

        World world = Bukkit.getWorld("world");

        if (world == null) {
            player.sendMessage("§cDie Overworld wurde nicht gefunden.");
            return true;
        }

        if (!player.getWorld().equals(world)) {
            player.sendMessage("§cRTP funktioniert nur in der Overworld.");
            return true;
        }

        long jetzt = System.currentTimeMillis();

        if (cooldown.containsKey(player.getUniqueId())) {

            long letzteBenutzung = cooldown.get(player.getUniqueId());

            long warten =
                    getConfig().getLong("cooldown", 5)
                    - (jetzt - letzteBenutzung) / 1000;

            if (warten > 0) {
                player.sendMessage(
                        "§cWarte noch " + warten + " Sekunden."
                );
                return true;
            }
        }

        player.sendMessage("§7Suche einen sicheren Ort...");

        Location ziel = findeOrt(world);

        if (ziel == null) {
            player.sendMessage("§cKein sicherer Ort gefunden.");
            return true;
        }

        player.teleport(ziel);

        cooldown.put(player.getUniqueId(), jetzt);

        player.sendMessage("§aDu wurdest zufällig teleportiert!");

        return true;
    }

    private Location findeOrt(World world) {

        // Radius vom Nullpunkt (0,0) - Standard 250000
        double radius = getConfig().getDouble("radius", 250000);

        double minX = -radius;
        double maxX = radius;

        double minZ = -radius;
        double maxZ = radius;

        int versuche = getConfig().getInt("max-attempts", 50);

        for (int i = 0; i < versuche; i++) {

            double x = minX + random.nextDouble() * (maxX - minX);
            double z = minZ + random.nextDouble() * (maxZ - minZ);

            int blockX = (int) x;
            int blockZ = (int) z;

            int y = world.getHighestBlockYAt(blockX, blockZ);

            Location ort = new Location(
                    world,
                    x,
                    y + 1,
                    z
            );

            if (sicher(ort)) {
                return ort;
            }
        }

        return null;
    }

    private boolean sicher(Location ort) {

        Material boden = ort.clone()
                .subtract(0, 1, 0)
                .getBlock()
                .getType();

        Material unten = ort.getBlock().getType();

        Material oben = ort.clone()
                .add(0, 1, 0)
                .getBlock()
                .getType();

        if (!boden.isSolid()) {
            return false;
        }

        if (unten.isSolid() || oben.isSolid()) {
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

        if (unten == Material.WATER ||
                unten == Material.LAVA) {
            return false;
        }

        if (oben == Material.WATER ||
                oben == Material.LAVA) {
            return false;
        }

        return true;
    }
}
