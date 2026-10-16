scoreboard players set #global azure_thorn.timer 0

# Held in main hand: blue trial-omen skulls drifting around the blade (right side, in front of the player)
execute as @a[gamemode=!spectator] if items entity @s weapon.mainhand *[minecraft:custom_data~{azure_thorn:1b}] at @s anchored eyes run particle minecraft:trial_omen ^-0.45 ^-0.35 ^0.55 0.2 0.35 0.2 0 1
execute as @a[gamemode=!spectator] if items entity @s weapon.mainhand *[minecraft:custom_data~{azure_thorn:1b}] at @s anchored eyes run particle minecraft:soul_fire_flame ^-0.45 ^-0.2 ^0.55 0.1 0.25 0.1 0.005 1

# Held in off hand
execute as @a[gamemode=!spectator] if items entity @s weapon.offhand *[minecraft:custom_data~{azure_thorn:1b}] at @s anchored eyes run particle minecraft:trial_omen ^0.45 ^-0.35 ^0.55 0.2 0.35 0.2 0 1

# Dropped on the ground
execute as @e[type=minecraft:item] if items entity @s contents *[minecraft:custom_data~{azure_thorn:1b}] at @s run particle minecraft:trial_omen ~ ~0.4 ~ 0.25 0.25 0.25 0 1
