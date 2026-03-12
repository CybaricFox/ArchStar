ArchStar is a small tech mod that adds power generation, machines, and tools. The mod is in early development and more content is to come.

Inspired by IndustrialCraft

Currently includes:
- A workbench for making items from the mod.
- 2 Furnaces.
- A Grinder to double ores.
- A Generator for producing power.
- Cables for power transport.
- Conveyors for item transport
- 2 ores.
- Rubber Trees (When V2 is released)
- A steel drill (Functionality coming in the next beta).

<img width="1649" height="851" alt="Screenshot 2026-02-15 184056" src="https://github.com/user-attachments/assets/62c19e3f-6c61-409b-b661-d92d9e525f9b" />

Latest Beta Build:
03/11/26 - beta 0.2.0

CONVEYOR UPDATE
- Conveyors allow transporting items from machines to storage to machines and so on. Conveyors support ArchStar machines, Vanilla processing benches, and chests.
- Stone conveyors are the earliest obtainable conveyor. Items take 5 seconds to pass through.
- Gold conveyors are more difficult to craft, but take only 2 seconds to pass through.
- Routers allow items to transfer in vertical directions and allow for multiple outputs.
- Conveyor importers are needed to extract items while exporters are needed to insert items.

Changes:

CONVEYORS
- Added Gold Conveyors and Routers
- Retextured Export Conveyor
- Moved Conveyor files to their own folders
- Conveyor blocks now drop items when destroyed.
- Conveyor importers now generate items. Items will follow the conveyor belt. They cannot be picked up.
- Conveyors will now activate the conveyor they target for better animation.
- Changed Importer recipe to be less evil.

BACKEND
- Renamed FoxLibrary to ArchLibrary
- Added DirectionLibrary

FIXES
- Fixed particle color of Tin and Ancient Salt
- Added Energy Component to debugger tool

Plans for beta 3:
Energy Update
- Add power storage
- Add functionality for restoring durability with power. (Adding a tag to items and then having the storage check for the tag is probably the best way to go.)
- Potentially refactor the whole energy system
