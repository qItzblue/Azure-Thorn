# Azure Thorn

Minecraft 1.21 – 1.21.1.

- `resourcepack/` – the model and texture (a retexture of Linaryx's Blood Scythe, applied to the netherite axe). Zip its contents and put it in `resourcepacks/`.
- `datapack/` – the item and its particles. Copy it into `<world>/datapacks/` and run `/reload`.

Run `/function azure_thorn:give` to get the weapon (Sharpness V, Unbreaking III, Mending, Fire Aspect II, Breach III, attack speed 2.0).

Particles: blue trial-omen skulls from the trial chambers (plus a few soul flames) float around the blade when the axe is held in either hand or lies on the ground. The pack tracks the item by its `custom_data` tag, so ordinary netherite axes get no particles.
