package dev.azurethorn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/** Builds and recognises the Azure Thorn item (a netherite axe retextured by the resource pack). */
public final class AzureThornItem {

    public static final int CUSTOM_MODEL_DATA = 7001;

    private final NamespacedKey key;
    private final NamespacedKey damageKey;
    private final NamespacedKey speedKey;

    public AzureThornItem(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "azure_thorn");
        this.damageKey = new NamespacedKey(plugin, "damage");
        this.speedKey = new NamespacedKey(plugin, "speed");
    }

    public ItemStack create() {
        ItemStack stack = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = stack.getItemMeta();

        meta.displayName(Component.text("Azure Thorn", NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                line("Right-click: ", "Thorn Retribution (30s)"),
                line("Sneak + right-click: ", "Chain Descent (15s)"),
                line("Passive: ", "every 5th hit chains the target")));

        // Sharpness and Breach are normally incompatible, so add them unsafely.
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        meta.addEnchant(Enchantment.BREACH, 3, true);

        // Keep the netherite axe's 10 damage; attack speed 2.0 (a sword is 1.6).
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                damageKey, 9.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
                speedKey, -2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));

        // Resource pack swaps in the Azure Thorn model only for this value.
        meta.setCustomModelData(CUSTOM_MODEL_DATA);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean is(ItemStack stack) {
        if (stack == null || stack.getType() != Material.NETHERITE_AXE || !stack.hasItemMeta()) return false;
        return stack.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private static Component line(String label, String text) {
        return Component.text(label, NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(text, NamedTextColor.GRAY));
    }
}
