[![](https://jitpack.io/v/sakura-ryoko/litematica.svg)](https://jitpack.io/#sakura-ryoko/litematica)

Litematica
==============
Litematica is a client-side Minecraft mod using Fabric.
It is more or less a re-creation of or a substitute for [Schematica](https://minecraft.curseforge.com/projects/schematica),
for players who don't want to have Forge installed.
For compiled builds (= downloads), see https://github.com/mms0316/litematica/releases/

Compiling
=========
* Clone the repository
* Open a command prompt/terminal to the repository directory
* run 'gradlew build'
* The built jar file will be in build/libs/

mms' fork
==============
This is a fork with feature additions. There are no plans to backport.

Features:
* EasyPlace: Addition of 'easyPlaceProtocolVersion' "Restricted" config
  * This is the default when using "easyPlaceProtocolVersion" set to "Auto"
  * Restricts placing blocks that requires specific orientation from the player
    * Stairs, Trapdoors, Doors, Dripleaves
    * Redstone components
    * Banners, Heads
    * Lanterns, Bells
    * Lava Buckets, Water Buckets
  * Places blocks that would normally require specific orientation
    * Hoppers, Logs
  * Fixes placements offset by one
    * Sculk Veins, Glow Lichen
* EasyPlace: Addition of config 'substitutions'
  * This allows e.g. Grass Blocks to substitute for Dirt
  * This is very rudimentary. There are issues with Material List
* EasyPlace: Addition of configs 'easyPlaceIgnoreEnderChest' and 'easyPlaceIgnoreShulkerBox'
  * Allows placing Ender Chests and Shulker Boxes inside schematic placements
* EasyPlace: Addition of config 'easyPlaceLeaveOne'
  * Restricts using up all item stacks of a material
  * This helps refilling inventory when combining wth inventory mods
* EasyPlace: Addition of config 'easyPlacePlaceInterval'
  * This allows reducing the 2000ms timer that stops block placements on a same previous location
  * The default value has been changed from 2000ms (official) to 400ms
  * This is helpful if you try to EasyPlace when you're inside a schematic block and then have to wait those 2s
* EasyPlace: Addition of config 'easyPlaceUseInterval'
  * Useful when having high ping
  * Restricts right-clicking too fast on existing blocks (Repeaters, Note Blocks, etc.)
* EasyPlace: Addition of hotkey 'easyPlaceFirstToggle'
  * Toggles 'easyPlaceFirst'
* EasyPlace: Addition of warning "Ran out of X" when using up the last item of a material including shulker boxes
  * If new config 'highlightRefillInInventory' is not disabled, lacking item in containers will be highlighted
  * Color may be controlled by new config 'highlightRefillInInventoryColor'
* EasyPlace: Addition of warning "Refill X" when using up the last item of a material outside shulker boxes
  * If new config 'highlightRefillInInventory' is not disabled, lacking item in containers will be highlighted
  * Color may be controlled by new config 'highlightRefillInInventoryColor'
  * If config 'blockInfoLinesEnabled' is not disabled, lacking items will be shown as an Info Overlay
  * If hotkey 'refillListClear' is pressed, the list of lacking items will be cleared
* EasyPlace: Addition of config 'easyPlacePickBlockHalt'
  * Reduces player speed if Pick Block fails or picks a Shulker Box
* EasyPlace and Material List: Addition of configs 'easyPlaceAvoidBeacons' and 'materialListAvoidBeacons', and hotkeys 'beaconRegister', 'beaconUnregister' and 'beaconUnregisterAll'
  * First, register beacons by pointing to them and hitting the hotkey
  * Then, blocks that obstruct registered beacons won't be EasyPlaced or won't be counted in Material List
* Render Layers: Addition of config 'layerMoveAmount'
  * Useful for map arts
  * If you have materials in your inventory for multiple layers, you could move that same amount of layers
* Material List: Addition of config 'materialListWriteSplitMeasures'
  * When using "Write to file", adds more columns separating shulker box, stacks and remainder amounts
* Material List: Addition of config 'materialListContainerOverlayEnabled'
  * After registering containers with hotkey 'materialListContainerRegister', creates outlines on containers that have materials that match the Material List
  * Use hotkeys 'materialListContainerUnregister' or 'materialListContainerUnregisterAll' to unregister
  * May use materialListFetchContainerColor to change the outline's color and transparency
* Material List: Addition of config 'materialListHotkeyAutoRefresh' to stop counting materials automatically when using hotkey 'openGuiMaterialList' for the first time
  * May be useful for schematics that go beyond viewing distance
* Material List: Addition of hotkeys 'materialListFetch' and 'materialListFetchKeepStacks'
  * When using hotkey with a container opened, all materials matching the Material List are transferred to player's inventory
  * 'materialListFetch' fetches to empty slots, while 'materialListFetchKeepStacks' does not'
* Material List: Addition of hotkey 'materialListRefresh'
  * This is a shortcut for M+L and "Refresh"
* Material List: Addition of hotkey 'materialListToggleInfoHud'
  * This is a shortcut for M+L and button click on "Info HUD: ON / OFF"
* Material List: Addition of counts of shulker boxes
  * If config 'materialListUseBSIFormat' is enabled, counts are shown in B (shulker boxes) S (stacks) I (remainder items) format
* Material List from Schematic Placement: Addition of config 'materialListPlacementPersistent' to change Material List to act like Schematic Verifier, at the cost of increased CPU and memory usage
  * Related task is now always running
  * Considers block updates
  * Doesn't need all chunks to be loaded before showing results
* Rendering: Addition of config 'schematicOverlayColorMissing2' to be alternated when using RenderLayers with axis
* Schematic Placement: Addition of hotkey 'setSchematicOrigin'
  * This moves the active schematic placement to player's position
  * This is a shortcut for - (minus key) and "Move to player"
* (Before 1.21.4 only) Schematic Loading: Addition of metadata preview for .schem and .nbt
* Schematic Loading: Addition of custom embedded image preview for .schem and .nbt
* Schematic Verifier: Addition of config 'schematicVerifierCheckChunkReload'
  * Useful for building with multiple people
  * Keeps checking for block changes outside render distance
* Schematic Verifier: Addition of cardinal coordinates for entries in Info Hud
* Schematic Verifier GUI: Addition of search bar

Tweaks:
* EasyPlace: Prestocks main hand using a single shift+click packet
  * This is different from Tweakeroo's hand prestock, which often uses two packets
  * This reduces chance of placing wrong blocks
* EasyPlace: Addition of support for ranges in 'pickBlockableSlots'
  * e.g. use "3-6" instead of "3,4,5,6"
  * Brought from 1.12.2 official branch
* EasyPlace: Skips handling InventoryS2CPacket (for player inventory) for 'easyPlaceSkipInventoryUpdateDuration' (default 100ms)
  * This fixes wrong block placements when being too fast
* Material List: Removal of message when refreshing
* Material List: No longer is cleared when changing dimensions
* Pick Block Shulkers: Prefers hotbar then the Shulker Box with the least amount of items
* Schematic Loading: Removal of warning when loading non .litematic schematics
* Schematic Loading: Removal of non-important metadata preview
  * Time created / modified when zero
  * Region count when not a .litematic
* Schematic Verifier: Keeps running even when the verifier checks all chunks
* Status Info HUD: Shows if easyPlaceFirst is true/false

Fixes:
* EasyPlace: Allows right-clicking to set block states (e.g. Note Blocks) without needing to turn off EasyPlace
  * Brought from 1.12.2 official branch
* Material List, Area Analyzer: Fixed having multiple unfinished tasks when changing layers or refreshing
* (Before 1.21.4 only) Schematic Verifier: Considers exploded blocks
* Schematic Verifier: When unloading a schematic placement, stops the related Verifier
* Task Scheduler: Brought synchronization fix from 1.12.2
