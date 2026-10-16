# Spawn particles every 2 ticks so it stays visible without spamming
scoreboard players add #global azure_thorn.timer 1
execute if score #global azure_thorn.timer matches 2.. run function azure_thorn:particles
