# VoxelMap

Minimap and world map for Minecraft. Clean, fast, no clutter.

---

## Available for

[![Fabric](https://raw.githubusercontent.com/Updated-VoxelMap/VoxelMap/master/modrinth-assets/badge_fabric.png)](https://modrinth.com/mod/voxelmap-updated/versions?l=fabric) &nbsp;&nbsp; [![Forge](https://raw.githubusercontent.com/Updated-VoxelMap/VoxelMap/master/modrinth-assets/badge_forge.png)](https://modrinth.com/mod/voxelmap-updated/versions?l=forge) &nbsp;&nbsp; [![NeoForge](https://raw.githubusercontent.com/Updated-VoxelMap/VoxelMap/master/modrinth-assets/badge_neoforge.png)](https://modrinth.com/mod/voxelmap-updated/versions?l=neoforge)

---

## About VoxelMap

VoxelMap always shows you where you are. The minimap runs alongside the game, and the fullscreen world map shows everything you've explored so far. On top of that: waypoints, radar, and a few tools that make exploring and building easier.

---

## Features

### Minimap & World Map
Compact minimap in the corner, fullscreen world map with a keypress. Zoom levels are freely adjustable, rotation with the player is optional.

![Minimap](https://raw.githubusercontent.com/Updated-VoxelMap/VoxelMap/master/modrinth-assets/Minimaps.png)

![World Map](https://raw.githubusercontent.com/Updated-VoxelMap/VoxelMap/master/modrinth-assets/Worldmap.png)

### Waypoints
Set your own markers, give them an icon and color, and toggle visibility per dimension. Waypoints can be shared with other players in chat.

![Waypoint menu](https://raw.githubusercontent.com/Updated-VoxelMap/VoxelMap/master/modrinth-assets/WaypointMenu.png) &nbsp;&nbsp; ![Waypoint in the world](https://raw.githubusercontent.com/Updated-VoxelMap/VoxelMap/master/modrinth-assets/WaypointInWorld.png)

### Cave Mode
Underground, the map automatically switches to a cave view once the surface is blocked from view.

### Radar
Shows nearby players and creatures. Names, helmets, and facing direction can be toggled individually, sneaking players can be hidden.

### Biomes & Slime Chunks
Biomes are color-coded on the map or shown as an overlay. Slime chunks can be made visible, on multiplayer servers with the correct seed.

### Deathpoints
A waypoint is automatically placed at your death location. Configurable to keep only the most recent one or all of them.

### Server Identity
Networks with multiple addresses or proxies can send the client a fixed identity, so waypoints and map data stay the same across every address. Custom alias mappings can also be set manually.

---

## For Server Owners: Paper Plugin

A matching plugin is available for Paper servers to restrict and configure VoxelMap server-side, without players needing to install anything. Configurable options include:

- Allow or block minimap, world map, and waypoints individually
- Toggle radar for players and mobs separately
- Allow or disable cave mode
- Enable or disable deathpoints
- Custom teleport command for waypoints
- Fixed server identity, so waypoints and map data persist across multiple addresses or proxies
- Per-world setting overrides

<details>
<summary>Example: config.json</summary>

```json
{
  "defaultConfig": {
    "radarAllowed": true,
    "radarMobsAllowed": true,
    "radarPlayersAllowed": true,
    "cavesAllowed": true,
    "minimapAllowed": true,
    "worldmapAllowed": true,
    "waypointsAllowed": true,
    "deathWaypointAllowed": true,
    "teleportCommand": "tppos",
    "worldNameSuffix": "",
    "serverIdentity": "MyNetwork"
  },
  "worldOverrides": [
    {
      "worlds": ["world_the_end"],
      "settings": {
        "radarAllowed": false,
        "cavesAllowed": false
      }
    }
  ]
}
```

</details>

---

## Requirements

Runs on Fabric, Forge, and NeoForge. Client-side only, no server installation required. The Paper plugin is optional and only intended for server owners who want to restrict the map.

---

[![GitHub](https://raw.githubusercontent.com/Updated-VoxelMap/VoxelMap/master/modrinth-assets/badge_github.png)](https://github.com/Updated-VoxelMap/VoxelMap) &nbsp;&nbsp; [![Discord](https://raw.githubusercontent.com/Updated-VoxelMap/VoxelMap/master/modrinth-assets/badge_discord.png)](https://discord.gg/4nGrfZxb2a)
