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
* EasyPlace: Addition of config 'easyPlaceUseInterval'
  * Useful when having high ping
  * Restricts right-clicking too fast on existing blocks (Repeaters, Note Blocks, etc.)
* EasyPlace: Addition of hotkey 'easyPlaceFirstToggle'
  * Toggles 'easyPlaceFirst'
* EasyPlace: Addition of warning "Ran out of X" when using up the last item of a material including shulker boxes
  * If config 'highlightBlockInInventory' is enabled, lacking item in containers will be highlighted
* EasyPlace: Addition of warning "Refill X" when using up the last item of a material outside shulker boxes
  * If config 'highlightBlockInInventory' is enabled, lacking item in containers will be highlighted
  * If config 'blockInfoLinesEnabled' is not disabled, lacking item will be shown as an Info Overlay
* Render Layers: Addition of config 'layerMoveAmount'
  * Useful for map arts
  * If you have materials in your inventory for multiple layers, you could move that same amount of layers
* Material List: Addition of config 'materialListWriteSplitMeasures'
  * When using "Write to file", adds more columns separating shulker box, stacks and remainer amounts
* Material List: Addition of config 'materialListContainerOverlayEnabled'
  * After registering containers with hotkey 'materialListContainerRegister', creates outlines on containers that have materials that match the Material List
  * Use hotkeys 'materialListContainerUnregister' or 'materialListContainerUnregisterAll' to unregister
  * May use materialListFetchContainerColor to change the outline's color and transparency
* Material List: Addition of hotkey 'materialListFetch'
  * When using hotkey with a container opened, all materials matching the Material List are transferred to player's inventory
* Material List: Addition of hotkey 'materialListRefresh'
  * This is a shortcut for M+L and "Refresh"
* Material List: Addition of counts of shulker boxes
* Schematic Placement: Addition of hotkey 'setSchematicOrigin'
  * This moves the active schematic placement to player's position
  * This is a shortcut for - (minus key) and "Move to player"
* Schematic Loading: Addition of metadata preview for .schem and .nbt
* Schematic Loading: Addition of custom embedded image preview for .schem and .nbt
* Schematic Verifier: Addition of config 'schematicVerifierCheckChunkReload'
  * Useful for building with multiple people
  * Keeps checking for block changes outside render distance
* Schematic Verifier: Addition of cardinal coordinates for entries in Info Hud

Tweaks:
* EasyPlace: Prestocks main hand using a single shift+click packet
  * This is different from Tweakeroo's hand prestock, which often uses two packets
  * This reduces chance of placing wrong blocks
* EasyPlace: Addition of support for ranges in 'pickBlockableSlots'
  * e.g. use "3-6" instead of "3,4,5,6"
  * Brought from 1.12.2 official branch
* Material List: Removal of message when refreshing
* Schematic Loading: Removal of warning when loading non .litematic schematics
* Schematic Loading: Removal of non-important metadata preview
  * Time created / modified when zero
  * Region count when not a .litematic
* Schematic Verifier: Keeps running even when the verifier checks all chunks

Fixes:
* EasyPlace: Allows right-clicking to set block states (e.g. Note Blocks) without needing to turn off EasyPlace
  * Brought from 1.12.2 official branch
* Schematic Verifier: Considers exploded blocks
* Schematic Verifier: When unloading a schematic placement, stops the related Verifier
* Task Scheduler: Brought synchronization fix from 1.12.2
