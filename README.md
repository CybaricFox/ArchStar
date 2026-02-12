A Hytale mod that I'm making to learn Hytale modding.

Currently includes:
A workbench for making items from the mod.
2 Furnaces.
A Grinder to double ores.
A Generator for producing power.

<img width="2560" height="1369" alt="Hytale2026-01-16_04-35-17" src="https://github.com/user-attachments/assets/d939ee8f-8fe8-4c1a-b53d-5bfb94f3f416" />

02/11/26 - alpha 0.0.2

Added Energy Systems:
- Energy Blocks join networks to more easily find their connected pals
- Energy Blocks can generate and consume energy.
- The Custom Processing System utilizes the same systems as the normal processing system with additional, customizable capabilities.
- Energy Updates in UIs

Added Common UI Page:
- This UI Page is designed for modular UI creation. Its capabilities are simple but useful.
- Functional item grids. The inventory grid accurately reflects your inventory and is mostly interactable.

Added Powered Processing:
- Grinder and Furnace now use electricity.
- Powered Processors use the same recipe system as normal processors.
- Recipes will only make progress if enough energy is supplied.

Rebalanced processing recipes.
Rebalanced crafting recipes.

Known Issues:
- The Item Grids will sometimes cancel interactions. This usually occurs on the same frame a sendUpdate is called. There is currently no fix that I am aware of.
- Shift click is not yet setup.
- Input UI is missing a progress bar.
- Fuel UI is missing a progress bar.
- Item Grids are still using placeholders.
- All Machine States are currently non-functional.
- All Machine Sounds are currently non-functional.
- Machines do not generate particles.

0.0.3 Plans:
- 0.0.3 will focus on adding cables so generators do not need to be placed next to consumers all the time.
- The above issues are the current priority.

![HytaleClient_U9PZm4w1Wf](https://github.com/user-attachments/assets/0df23e36-ef04-4cbf-8a68-0036111b8a34)
