package dev.azurethorn;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class AzureThornPlugin extends JavaPlugin {

    private AzureThornItem item;

    @Override
    public void onEnable() {
        item = new AzureThornItem(this);
        AbilityListener abilities = new AbilityListener(this, item);
        getServer().getPluginManager().registerEvents(abilities, this);
        new ParticleTask(item).runTaskTimer(this, 1L, 2L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("Usage: /azurethorn give [player]");
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("Console must name a player: /azurethorn give <player>");
            return true;
        }
        target.getInventory().addItem(item.create());
        sender.sendMessage("Gave Azure Thorn to " + target.getName() + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("give");
        if (args.length == 2) return null; // online player names
        return List.of();
    }
}
