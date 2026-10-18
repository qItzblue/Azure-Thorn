package dev.azurethorn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class AbilityListener implements Listener {

    // Ability 1: Thorn Retribution
    private static final long RETRIBUTION_COOLDOWN_MS = 30_000;
    private static final int RETRIBUTION_DURATION_TICKS = 5 * 20;
    private static final double RETRIBUTION_RADIUS = 3.5;      // 7x7 area
    private static final double RETRIBUTION_MULTIPLIER = 2.0;

    // Ability 2: Chain Descent
    private static final long DESCENT_COOLDOWN_MS = 15_000;
    private static final double DESCENT_RADIUS = 5.0;          // 10x10 area
    private static final int DENSITY_LEVEL = 5;

    // Passive: Thorn Chain
    private static final int PASSIVE_HITS = 5;
    private static final long COMBO_TIMEOUT_MS = 3_000;
    private static final double PASSIVE_DAMAGE = 14.0;         // 7 hearts
    private static final int REACH_DURATION_TICKS = 10 * 20;
    private static final double REACH_BONUS = 2.0;             // 3 -> 5 blocks

    private static final BlockData CHAIN = Material.CHAIN.createBlockData();

    private final JavaPlugin plugin;
    private final AzureThornItem item;
    private final NamespacedKey reachKey;

    private final Map<UUID, Long> retributionReady = new HashMap<>();
    private final Map<UUID, Long> descentReady = new HashMap<>();
    /** Damage soaked up while Thorn Retribution is active. */
    private final Map<UUID, Double> absorbing = new HashMap<>();
    /** Players mid Chain Descent; their fall damage is cancelled. */
    private final Set<UUID> descending = new HashSet<>();
    private final Map<UUID, Integer> comboHits = new HashMap<>();
    private final Map<UUID, Long> lastHit = new HashMap<>();
    private final Map<UUID, BukkitTask> reachTasks = new HashMap<>();
    /** Set while the plugin itself deals damage, so it doesn't count as a combo hit. */
    private boolean dealingAbilityDamage;

    AbilityListener(JavaPlugin plugin, AzureThornItem item) {
        this.plugin = plugin;
        this.item = item;
        this.reachKey = new NamespacedKey(plugin, "reach");
    }

    // ------------------------------------------------------------------ activation

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        if (!item.is(p.getInventory().getItemInMainHand())) return;

        event.setCancelled(true); // no log stripping etc.
        if (p.isSneaking()) {
            if (checkCooldown(p, descentReady, "Chain Descent")) startDescent(p);
        } else {
            if (checkCooldown(p, retributionReady, "Thorn Retribution")) startRetribution(p);
        }
    }

    private boolean checkCooldown(Player p, Map<UUID, Long> readyAt, String name) {
        long left = readyAt.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis();
        if (left > 0) {
            p.sendActionBar(Component.text(name + " ready in " + (left + 999) / 1000 + "s", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ Ability 1

    private void startRetribution(Player p) {
        UUID id = p.getUniqueId();
        if (absorbing.containsKey(id)) return;
        retributionReady.put(id, System.currentTimeMillis() + RETRIBUTION_COOLDOWN_MS);
        absorbing.put(id, 0.0);
        p.sendActionBar(Component.text("Thorn Retribution: absorbing damage...", NamedTextColor.AQUA));
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1f, 1.2f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!p.isOnline() || p.isDead()) {
                    absorbing.remove(id);
                    cancel();
                    return;
                }
                if (ticks++ % 4 == 0) {
                    p.getWorld().spawnParticle(Particle.TRIAL_OMEN, p.getLocation().add(0, 1, 0), 6, 0.5, 0.8, 0.5, 0);
                }
                if (ticks >= RETRIBUTION_DURATION_TICKS) {
                    cancel();
                    explode(p, absorbing.remove(id));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explode(Player p, double absorbed) {
        Location c = p.getLocation();
        World w = p.getWorld();
        w.spawnParticle(Particle.EXPLOSION_EMITTER, c, 1);
        w.spawnParticle(Particle.TRIAL_OMEN, c.clone().add(0, 1, 0), 60, RETRIBUTION_RADIUS / 2, 1, RETRIBUTION_RADIUS / 2, 0.05);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 0.5, 0), 80, RETRIBUTION_RADIUS / 2, 0.5, RETRIBUTION_RADIUS / 2, 0.1);
        w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);

        double damage = absorbed * RETRIBUTION_MULTIPLIER;
        p.sendActionBar(Component.text("Thorn Retribution released " + String.format("%.1f", damage) + " damage!", NamedTextColor.AQUA));
        if (damage <= 0) return;

        DamageSource source = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                .withCausingEntity(p).withDirectEntity(p).withDamageLocation(c).build();
        for (LivingEntity target : nearby(p, c, RETRIBUTION_RADIUS)) {
            dealDamage(target, damage, source);
        }
    }

    // ------------------------------------------------------------------ Ability 2

    private void startDescent(Player p) {
        UUID id = p.getUniqueId();
        if (descending.contains(id)) return;
        descentReady.put(id, System.currentTimeMillis() + DESCENT_COOLDOWN_MS);
        descending.add(id);
        p.setVelocity(p.getVelocity().setY(1.6));
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.3f);

        new BukkitRunnable() {
            int ticks = 0;
            boolean pulling = false;
            double peakY = p.getLocation().getY();

            @Override
            public void run() {
                ticks++;
                if (!p.isOnline() || p.isDead() || ticks > 100) {
                    finish();
                    return;
                }
                Location loc = p.getLocation();
                peakY = Math.max(peakY, loc.getY());

                if (!pulling) {
                    // Rise until the launch tops out, then fire the chain at the floor.
                    if (ticks > 3 && p.getVelocity().getY() <= 0.1) {
                        pulling = true;
                        p.getWorld().playSound(loc, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.6f);
                    }
                    return;
                }

                Location ground = groundBelow(loc);
                drawChain(loc.clone().add(0, 0.8, 0), ground);
                p.setVelocity(new Vector(0, -2.5, 0));
                p.setFallDistance(0);

                if (ticks > 5 && (isOnGround(p) || loc.getY() - ground.getY() < 0.2)) {
                    slam(p, Math.max(0, peakY - loc.getY()));
                    finish();
                }
            }

            private void finish() {
                cancel();
                // Keep fall damage off for a moment after landing.
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> descending.remove(id), 10L);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Mace smash damage (1.21) with Density 5 for the given fall distance. */
    static double maceDamage(double fall) {
        double smash;
        if (fall <= 3) smash = 4 * fall;
        else if (fall <= 8) smash = 12 + 2 * (fall - 3);
        else smash = 22 + (fall - 8);
        return 6 + smash + 0.5 * DENSITY_LEVEL * fall;
    }

    private void slam(Player p, double fall) {
        Location c = p.getLocation();
        World w = p.getWorld();
        w.spawnParticle(Particle.EXPLOSION, c, 6, 2, 0.2, 2, 0);
        w.spawnParticle(Particle.BLOCK, c, 120, DESCENT_RADIUS / 2, 0.2, DESCENT_RADIUS / 2, 0, c.clone().subtract(0, 0.5, 0).getBlock().getBlockData());
        w.spawnParticle(Particle.TRIAL_OMEN, c.clone().add(0, 0.5, 0), 40, DESCENT_RADIUS / 2, 0.3, DESCENT_RADIUS / 2, 0);
        w.playSound(c, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5f, 0.9f);

        double damage = maceDamage(fall);
        DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(p).withDirectEntity(p).build();
        for (LivingEntity target : nearby(p, c, DESCENT_RADIUS)) {
            dealDamage(target, damage, source);
            Vector push = target.getLocation().toVector().subtract(c.toVector()).setY(0);
            if (push.lengthSquared() > 1e-4) push.normalize().multiply(0.6);
            target.setVelocity(push.setY(0.5));
        }
    }

    // ------------------------------------------------------------------ damage hooks

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        UUID id = p.getUniqueId();

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && descending.contains(id)) {
            event.setCancelled(true);
            return;
        }
        Double stored = absorbing.get(id);
        if (stored != null) {
            EntityDamageEvent.DamageCause cause = event.getCause();
            if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.KILL) return;
            absorbing.put(id, stored + event.getFinalDamage());
            event.setCancelled(true);
            p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 0.02);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (dealingAbilityDamage) return;
        if (!(event.getDamager() instanceof Player p)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;
        if (!item.is(p.getInventory().getItemInMainHand())) return;

        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        int hits = now - lastHit.getOrDefault(id, 0L) > COMBO_TIMEOUT_MS ? 1 : comboHits.getOrDefault(id, 0) + 1;
        lastHit.put(id, now);

        if (hits >= PASSIVE_HITS) {
            comboHits.put(id, 0);
            // Run next tick so it doesn't stack on the triggering hit's invulnerability frames.
            plugin.getServer().getScheduler().runTask(plugin, () -> thornChain(p, target));
        } else {
            comboHits.put(id, hits);
            p.sendActionBar(Component.text("Thorn Chain " + hits + "/" + PASSIVE_HITS, NamedTextColor.DARK_AQUA));
        }
    }

    private void thornChain(Player p, LivingEntity target) {
        if (!p.isOnline() || target.isDead()) return;
        drawChain(p.getEyeLocation().subtract(0, 0.3, 0), target.getLocation().add(0, target.getHeight() / 2, 0));
        p.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.7f);

        target.setNoDamageTicks(0);
        dealDamage(target, PASSIVE_DAMAGE, DamageSource.builder(DamageType.PLAYER_ATTACK)
                .withCausingEntity(p).withDirectEntity(p).build());
        target.setVelocity(target.getVelocity().setY(1.2));

        grantReach(p);
        p.sendActionBar(Component.text("Thorn Chain! +2 reach for 10s", NamedTextColor.AQUA));
    }

    private void grantReach(Player p) {
        AttributeInstance reach = p.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
        if (reach == null) return;
        reach.removeModifier(reachKey);
        reach.addTransientModifier(new AttributeModifier(reachKey, REACH_BONUS,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));

        BukkitTask old = reachTasks.remove(p.getUniqueId());
        if (old != null) old.cancel();
        reachTasks.put(p.getUniqueId(), plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            reachTasks.remove(p.getUniqueId());
            reach.removeModifier(reachKey);
        }, REACH_DURATION_TICKS));
    }

    @EventHandler
    public void onSwap(PlayerItemHeldEvent event) {
        comboHits.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        UUID id = p.getUniqueId();
        comboHits.remove(id);
        lastHit.remove(id);
        absorbing.remove(id);
        BukkitTask task = reachTasks.remove(id);
        if (task != null) task.cancel();
        AttributeInstance reach = p.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
        if (reach != null) reach.removeModifier(reachKey);
    }

    // ------------------------------------------------------------------ helpers

    private void dealDamage(LivingEntity target, double amount, DamageSource source) {
        dealingAbilityDamage = true;
        try {
            target.damage(amount, source);
        } finally {
            dealingAbilityDamage = false;
        }
    }

    private static Iterable<LivingEntity> nearby(Player p, Location c, double radius) {
        return c.getNearbyLivingEntities(radius, radius, radius,
                e -> e != p && !(e instanceof Player other && other.getGameMode().isInvulnerable()));
    }

    private static Location groundBelow(Location from) {
        Location l = from.clone();
        int minY = from.getWorld().getMinHeight();
        while (l.getY() > minY && l.getBlock().isPassable()) l.subtract(0, 1, 0);
        l.setY(l.getBlockY() + 1);
        return l;
    }

    @SuppressWarnings("deprecation")
    private static boolean isOnGround(Entity e) {
        return e.isOnGround();
    }

    private static void drawChain(Location from, Location to) {
        World w = from.getWorld();
        Vector step = to.toVector().subtract(from.toVector());
        double len = step.length();
        if (len < 0.1) return;
        step.normalize().multiply(0.4);
        Location at = from.clone();
        for (double d = 0; d < len; d += 0.4) {
            w.spawnParticle(Particle.BLOCK, at, 1, 0, 0, 0, 0, CHAIN);
            if (((int) (d / 0.4)) % 3 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, at, 1, 0, 0, 0, 0);
            at.add(step);
        }
    }
}
