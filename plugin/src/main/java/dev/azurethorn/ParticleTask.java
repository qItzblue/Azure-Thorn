package dev.azurethorn;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/** Blue trial-chamber skulls (trial_omen) drifting around the blade. */
final class ParticleTask extends BukkitRunnable {

    private final AzureThornItem item;

    ParticleTask(AzureThornItem item) {
        this.item = item;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            if (item.is(p.getInventory().getItemInMainHand())) spawnAtHand(p, -0.45);
            if (item.is(p.getInventory().getItemInOffHand())) spawnAtHand(p, 0.45);
        }
        for (World world : Bukkit.getWorlds()) {
            for (Item drop : world.getEntitiesByClass(Item.class)) {
                if (item.is(drop.getItemStack())) {
                    world.spawnParticle(Particle.TRIAL_OMEN, drop.getLocation().add(0, 0.4, 0), 1, 0.25, 0.25, 0.25, 0);
                }
            }
        }
    }

    /** side: negative = player's right hand, positive = left. */
    private static void spawnAtHand(Player p, double side) {
        Location eye = p.getEyeLocation();
        Vector forward = eye.getDirection().setY(0);
        if (forward.lengthSquared() < 1e-4) forward = new Vector(0, 0, 1);
        forward.normalize();
        Vector left = new Vector(forward.getZ(), 0, -forward.getX());
        Location at = eye.clone().add(forward.multiply(0.55)).add(left.multiply(side)).add(0, -0.35, 0);
        World w = p.getWorld();
        w.spawnParticle(Particle.TRIAL_OMEN, at, 1, 0.2, 0.35, 0.2, 0);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, at.add(0, 0.15, 0), 1, 0.1, 0.25, 0.1, 0.005);
    }
}
