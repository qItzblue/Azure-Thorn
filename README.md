# Azure Thorn

A Paper plugin and resource pack for **Minecraft 1.21.1**.

- `plugin/` – the Paper plugin. Build it with `mvn package` and copy `target/AzureThorn-1.0.0.jar` into the server's `plugins/` folder.
- `resourcepack/` – the model and texture (a retexture of Linaryx's Blood Scythe). It only changes netherite axes with custom model data 7001, which the plugin gives Azure Thorn, so normal axes look vanilla. Zip the folder's contents and use the zip as the server resource pack.

`/azurethorn give [player]` (op only) gives the weapon.

## The weapon
Netherite axe, 10 damage, attack speed 2.0 (a sword is 1.6). Sharpness V, Unbreaking III, Mending, Fire Aspect II, Breach III.
Blue trial-omen skulls float around the blade when it's held or dropped.

| Control | Ability | Cooldown |
|---|---|---|
| Right-click | **Thorn Retribution** – for 5 s you take no damage. When it ends, everything you would have taken explodes for 2× that damage in a 7×7 area. | 30 s |
| Sneak + right-click | **Chain Descent** – launches you up, then a chain drags you to the floor. Landing hits everything in a 10×10 area for Density V mace damage, based on the height you fell. | 15 s |
| Passive | **Thorn Chain** – every 5th hit in a row (no more than 3 s between hits) shoots a chain that launches the target and deals 7 hearts, and gives you 5-block reach for 10 s. | – |

Tune the numbers through the constants at the top of `AbilityListener.java`.
