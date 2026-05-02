# MythicSkyWars

A modern, high-performance SkyWars plugin built for Spigot and Paper servers, supporting Minecraft versions from 1.8 up to 26.1.2.

This project started as a hard fork of MythicSkywars but has evolved into a fully independent and heavily extended solution focused on performance, scalability, and modern features.

---

## ✨ Overview

MythicSkyWars is designed for both standalone servers and large proxy networks. It focuses on:
- Low resource usage
- Fast setup
- High configurability
- Seamless player experience

---

## 🚀 Core Features

- Multi-arena support with solo and team modes
- Full proxy compatibility (BungeeCord and Velocity)
- Advanced cage system with customizable spawn cages (including schematic cages via WorldEdit)
- Configurable chest loot system (basic, normal, overpowered + center variants)
- Voting system (time, weather, chest type, modifiers, health)
- Spectator mode with full controls
- Scoreboards with PlaceholderAPI support (5 different states)
- Kit system with in-game creation and voting
- Game events (EnderDragon, Wither, MobSpawn, CrateDrop, ChestRefill, DeathMatch, ArrowRain, AnvilRain, ShrinkingBorder, and more)
- Party system
- MySQL support
- Holographic leaderboards (DecentHolograms)
- Particle effects, projectile trails, taunts, kill/win sounds, glass colors
- Player statistics (wins, kills, losses, deaths, games played, XP, level, K/D, W/L)
- 100% configurable messages

---

## 🔥 MythicSkyWars Enhancements

### Gameplay & Systems
- **Rejoin System** — reconnect to active matches after disconnect (`/rejoin`)
- **Lucky Block Integration** — optional Lucky Block mode with NTD-LuckyBlock, custom drops, hand items, and dedicated menus
- **Prestige System** — multiple prestige tiers with requirements (level, kills, permissions), custom prefixes, and database tracking
- **Level & XP System** — full progression with configurable XP rewards per action, progress bars, level-up reward commands
- **Soul Well** — Hypixel-style soul well with animated spinning, rarity tiers (legendary/rare/common), soul currency
- **Team Balancing** — automatic distribution of unselected players across teams
- **Summoned Mob System** — mobs spawned via spawn eggs are friendly to the owner and their team, auto-target enemies
- **Water Portal Joining** — configurable water portal regions in lobby that auto-join players to games
- **Default Kit Files** — bundled starter kit configurations
- **Easy Arena Setup** — streamlined arena creation workflow

### Visual & UX Improvements
- **Chest Refill Visuals** — visual indicators with timers for chest refills
- **Lobby Enhancements** — hide join/quit/death messages, water portal system
- **Glass Color Options** — legacy-compatible cosmetics
- **Cosmetics Reorganization** — clean directory structure

### Backend & Developer Features
- **Config Auto-Merge** — safely syncs bundled config updates without overwriting existing values
- **MongoDB Migration** — import full player data from UltraSkyWars (kills, wins, XP, souls, levels, cosmetics, prestige)
- **Developer API Events** — SkyWarsDeathEvent, SkyWarsJoinEvent, SkyWarsKillEvent, SkyWarsLeaveEvent, SkyWarsWinEvent, SkyWarsMatchStateChangeEvent, SkyWarsVoteEvent, SkyWarsSelectKitEvent, SkyWarsSelectTeamEvent, SkyWarsReloadEvent, SkyWarsReloadPreLoadEvent, SkywarsGameEventTriggerEvent, SkywarsGameEventAnnounceEvent

### New Commands
| Command | Permission | Description |
|---------|-----------|-------------|
| `/sw prestige` | `sw.prestige` | Open prestige selection menu |
| `/rejoin` | `sw.rejoin` | Rejoin previous match if still available |
| `/leave` | `sw.leave` | Leave current match (standalone) |
| `/sw soulwell` | `sw.admin` | Configure soul well location |
| `/sw migrateusw` | `sw.migrateusw` | Import stats from UltraSkyWars MongoDB |

### New Permissions
| Permission | Description |
|-----------|-------------|
| `sw.rejoin` | Use /rejoin command |
| `sw.leave` | Use /leave command |
| `sw.prestige` | Use prestige menu |
| `sw.soulwell` | Use Soul Well from options menu |
| `sw.migrateusw` | Run UltraSkyWars migration |

### New Placeholders (PlaceholderAPI)
| Placeholder | Description |
|-------------|-------------|
| `%swr_souls%` | Total souls |
| `%swr_soulwell_usages%` | Soul well spins |
| `%swr_soulwell_legendaries%` | Legendary drops received |
| `%swr_soulwell_rares%` | Rare drops received |
| `%swr_soulwell_souls_gathered%` | Souls collected from gameplay |
| `%swr_soulwell_souls_purchased%` | Souls bought with currency |
| `%swr_prestige_prefix%` | Prestige tier prefix |
| `%swr_prestige_id%` | Current prestige ID |
| `%swr_level_display_prefix%` | Combined level + prestige display |
| `%swr_players_playing_solo%` | Solo players currently in-game |
| `%swr_players_playing_team%` | Team players currently in-game |
| `%swr_players_waiting_solo%` | Solo players waiting |
| `%swr_players_waiting_team%` | Team players waiting |

### New Config Files
- **`levels.yml`** — XP rewards, level definitions, progress bar settings, prestige tiers, level-up rewards
- **`luckyblocks.yml`** — Lucky Block mode settings, custom drops, hand items, chest loot integration

---

## 📦 Supported Versions

| Minecraft | NMS Module |
|-----------|-----------|
| 1.8.8 | v1_8_R3 |
| 1.9.x | v1_9_R1, v1_9_R2 |
| 1.10.2 | v1_10_R1 |
| 1.11.2 | v1_11_R1 |
| 1.12.2 | v1_12_R1 |
| 1.13.2 | v1_13_R2 |
| 1.14.4 | v1_14_R1 |
| 1.15.1 | v1_15_R1 |
| 1.16.x | v1_16_R1, v1_16_R2, v1_16_R3 |
| 1.17 | v1_17_R1 |
| 1.18.2 | v1_18_R2 |
| 1.19 | v1_19_R1 |
| 1.20.6 | v1_20_R1 |
| 1.21.1 | v1_21_R1 |
| 26.1.2 | v26_1_R1 |

---

## ⚙️ Requirements

### Required
- WorldEdit

### Optional
- Vault (economy support)
- DecentHolograms (hologram leaderboards)
- PlaceholderAPI (placeholder support)
- Multiverse-Core (world management)
- PartyAndFriends (BungeeCord party support)
- LuckyBlocksNTD (Lucky Block mode)

---

## 🛠 Installation

1. Build or download the plugin jar
2. Place the jar into your `plugins` folder
3. Start the server to generate configs
4. Set lobby spawn with `/sw setspawn`
5. Create arenas and configure settings
6. Restart and enjoy

---

## 📋 Commands

### Registered Base Commands
| Command | Aliases | Description |
|---------|---------|-------------|
| `/skywars` | `/sw` | Main plugin command |
| `/leave` | — | Leave current match (standalone) |
| `/rejoin` | — | Rejoin previous match |
| `/swparty` | `/swp` | Party commands |
| `/swkit` | `/swk` | Kit commands |
| `/swmap` | `/swm` | Map commands |

### Player Commands (`/sw ...`)
| Command | Description |
|---------|-------------|
| `/sw join` | Join a SkyWars match |
| `/sw joinmenu [solo\|team]` | Open the join menu |
| `/sw quit` | Quit from the current game |
| `/sw lobby` | Return to lobby |
| `/sw spectate [player/map]` | Spectate a game |
| `/sw stats [player]` | View player stats |
| `/sw top [stat]` | Display leaderboard |
| `/sw options` | Open options menu |
| `/sw prestige` | Open prestige selection menu |
| `/sw winsound` | Open win sound menu |
| `/sw killsound` | Open kill sound menu |
| `/sw glass` | Open glass color menu |
| `/sw taunt` | Open taunt menu |
| `/sw particle` | Open particle effect menu |
| `/sw projectile` | Open projectile effect menu |
| `/sw bypasslobbyslots` | Bypass lobby slot limits |

### Admin Commands (`/sw ...`)
| Command | Description |
|---------|-------------|
| `/sw setspawn` | Set the lobby spawn |
| `/sw reload` | Reload SkyWars |
| `/sw start` | Force start a match |
| `/sw stat [player] [stat] [method] [amount]` | Modify player stats |
| `/sw clearstats [player]` | Clear a player's stats |
| `/sw chestadd [type] [method] [%]` | Add items to chest loot |
| `/sw chestedit` | Edit chest loot in-game |
| `/sw updatetop` | Force update leaderboards |
| `/sw hologram [stat] [format]` | Add a hologram leaderboard |
| `/sw holoremove` | Remove closest hologram |
| `/sw soulwell` | Configure soul well location |
| `/sw lobbywaterportal` | Configure lobby water portals |
| `/sw migrateusw` | Import stats from UltraSkyWars MongoDB |
| `/sw send` | Send a player to a game |
| `/sw select` | Select an arena |

### Map Commands (`/swmap ...`)
| Command | Description |
|---------|-------------|
| `/swmap create [name]` | Create a new map |
| `/swmap edit [name]` | Open map for editing |
| `/swmap save [name]` | Save an open map |
| `/swmap register [name]` | Register map for play |
| `/swmap unregister [name]` | Remove map from play |
| `/swmap delete [name]` | Delete a map |
| `/swmap list` | List available maps |
| `/swmap arenas` | Open Arena GUI |
| `/swmap spawn [type] [team]` | Add spawn (player/spec/deathmatch/look) |
| `/swmap chesttype [name]` | Toggle chest type (normal/center) |
| `/swmap checkchest` | Verify chest placements |
| `/swmap minimum [name] [num]` | Set minimum players |
| `/swmap name [name] [display]` | Set display name |
| `/swmap creator [name] [creator]` | Set creator name |
| `/swmap teamsize [name] [size]` | Set team size |
| `/swmap refresh [name]` | Reload map data file |
| `/swmap legacyload [name]` | Import legacy beacon-based map |
| `/swmap leave` | Leave map editor |
| `/swmap debug` | Debug map info |

### Kit Commands (`/swkit ...`)
| Command | Description |
|---------|-------------|
| `/swkit create [name]` | Create kit from inventory |
| `/swkit update [name]` | Update kit with current inventory |
| `/swkit enable [name]` | Toggle kit enabled/disabled |
| `/swkit icon [name]` | Set kit icon to held item |
| `/swkit lockicon [name]` | Set locked icon to held item |
| `/swkit name [name] [display]` | Set display name |
| `/swkit lore [name] [line] [text]` | Set lore line (1-16 or "locked") |
| `/swkit position [name] [pos]` | Set menu position (0-44) |
| `/swkit perm [name]` | Toggle permission requirement |
| `/swkit list` | List all kits |
| `/swkit load [name]` | Preview kit in inventory |

### Party Commands (`/swparty ...`)
| Command | Description |
|---------|-------------|
| `/swparty create [name]` | Create a party |
| `/swparty invite [player]` | Invite a player |
| `/swparty accept` | Accept invite |
| `/swparty decline` | Decline invite |
| `/swparty leave` | Leave party |
| `/swparty disband` | Disband party |
| `/swparty info` | View party info |
| `/swparty name [name]` | Change party name |

---

## 🔑 Permissions

### Group Permissions
| Permission | Description |
|-----------|-------------|
| `sw.*` | All SkyWars permissions |
| `sw.admin` | All admin commands |
| `sw.maps` | All map commands |
| `sw.kits` | All kit commands |
| `sw.parties` | All party commands |
| `sw.player` | All player commands |

### Player Permissions
| Permission | Description |
|-----------|-------------|
| `sw.join` | Join games |
| `sw.joinmenu` | Use join menu |
| `sw.quit` | Quit games |
| `sw.leave` | Use /leave command |
| `sw.rejoin` | Use /rejoin command |
| `sw.spectate` | Spectate games |
| `sw.stats` | View stats |
| `sw.top` | View leaderboard |
| `sw.prestige` | Use prestige menu |
| `sw.soulwell` | Use Soul Well |
| `sw.soulwell.xezbethluck` | Soul well luck bonus |
| `sw.votemenu` | Open vote menu |
| `sw.chestvote` | Vote on chest type |
| `sw.healthvote` | Vote on health |
| `sw.timevote` | Vote on time |
| `sw.weathervote` | Vote on weather |
| `sw.modifiervote` | Vote on modifier |
| `sw.winsound` | Win sound menu |
| `sw.killsound` | Kill sound menu |
| `sw.glass` | Glass color menu |
| `sw.taunt` | Taunt menu |
| `sw.projectile` | Projectile effect menu |
| `sw.particle` | Particle effect menu |
| `sw.colorchat` | Use color codes in chat |
| `sw.signs` | Break/place SkyWars signs |
| `sw.vip1` – `sw.vip5` | VIP multiplier tiers |
| `sw.kit.[filename]` | Unlock specific kits |
| `sw.pareffect.[name]` | Unlock particle effects |
| `sw.proeffect.[name]` | Unlock projectile effects |
| `sw.glasscolor.[name]` | Unlock glass colors |
| `sw.killsound.[name]` | Unlock kill sounds |
| `sw.winsound.[name]` | Unlock win sounds |
| `sw.taunt.[name]` | Unlock taunts |

### Admin Permissions
| Permission | Description |
|-----------|-------------|
| `sw.setspawn` | Set lobby spawn |
| `sw.reload` | Reload plugin |
| `sw.start` | Force start match |
| `sw.stat` | Modify player stats |
| `sw.clearstats` | Clear player stats |
| `sw.chestadd` | Add chest loot |
| `sw.chesttype` | Change chest type |
| `sw.updatetop` | Update leaderboards |
| `sw.migrateusw` | Run UltraSkyWars migration |
| `sw.send` | Send player to game |
| `sw.select` | Select arena |
| `sw.allowcommands` | Use commands while in-game |
| `sw.admin.joinBypass` | Bypass full game restrictions |
| `sw.alterlobby` | Modify lobby |
| `sw.opteleport` | OP teleport |
| `sw.partychatspy` | Spy on party chat |

### Map Permissions
| Permission | Description |
|-----------|-------------|
| `sw.map.arenas` | Use Arenas GUI |
| `sw.map.create` | Create arenas |
| `sw.map.edit` | Edit arenas |
| `sw.map.delete` | Delete arenas |
| `sw.map.list` | List arenas |
| `sw.map.save` | Save arenas |
| `sw.map.register` | Register arenas |
| `sw.map.unregister` | Unregister arenas |
| `sw.map.minimum` | Set minimum players |
| `sw.map.name` | Set display name |
| `sw.map.creator` | Set creator |
| `sw.map.refresh` | Reload map data |

### Kit Permissions
| Permission | Description |
|-----------|-------------|
| `sw.kit.create` | Create kits |
| `sw.kit.update` | Update kits |
| `sw.kit.enable` | Enable/disable kits |
| `sw.kit.icon` | Set kit icon |
| `sw.kit.lockicon` | Set locked icon |
| `sw.kit.perm` | Toggle permission |
| `sw.kit.load` | Load/preview kits |
| `sw.kit.list` | List kits |
| `sw.kit.lore` | Set kit lore |
| `sw.kit.name` | Set kit name |
| `sw.kit.position` | Set menu position |

### Party Permissions
| Permission | Description |
|-----------|-------------|
| `sw.party.create` | Create parties |
| `sw.party.invite` | Invite players |
| `sw.party.accept` | Accept invites |
| `sw.party.decline` | Decline invites |
| `sw.party.leave` | Leave party |
| `sw.party.disband` | Disband party |
| `sw.party.info` | View party info |

---

## 📊 Placeholders

For PlaceholderAPI (use between `%` `%`):

| Placeholder | Description |
|-------------|-------------|
| `swr_wins` | Total wins |
| `swr_losses` | Total losses |
| `swr_kills` | Total kills |
| `swr_deaths` | Total deaths |
| `swr_xp` | XP amount |
| `swr_level` | XP level |
| `swr_elo` | ELO rating |
| `swr_games_played` | Games played |
| `swr_kill_death` | K/D ratio |
| `swr_win_loss` | W/L ratio |

### Scoreboard Placeholders

**Lobby:** `{wins}` `{losses}` `{kills}` `{deaths}` `{xp}` `{level}` `{killdeath}` `{winloss}` `{balance}`
**In-game:** `{players_needed}` `{waitingtimer}` `{nextevent_time}` `{nextevent_name}` `{kills}` `{mapname}` `{time}` `{aliveplayers}` `{players}` `{maxplayers}` `{winner}` `{winner1..8}` `{restarttime}` `{chestvote}` `{timevote}` `{healthvote}` `{weathervote}` `{modifiervote}`

---

## 🧑‍💻 Building

### For developers (with Nexus access)

```bash
git clone <repo-url>
cd MythicSkyWars
mvn clean package -DskipTests
```

Output: `target/MythicSkywars-<version>.jar`

## 📁 Project Structure

```
MythicSkywarsCore/    Main plugin logic
v1_8_R3/                NMS 1.8.8
v1_9_R1/                NMS 1.9.2
v1_9_R2/                NMS 1.9.4
v1_10_R1/               NMS 1.10.2
v1_11_R1/               NMS 1.11.2
v1_12_R1/               NMS 1.12.2
v1_13_R2/               NMS 1.13.2
v1_14_R1/               NMS 1.14.4
v1_15_R1/               NMS 1.15.1
v1_16_R1/               NMS 1.16.1
v1_16_R2/               NMS 1.16.2
v1_16_R3/               NMS 1.16.4
v1_17_R1/               NMS 1.17
v1_18_R2/               NMS 1.18.2
v1_19_R1/               NMS 1.19
v1_20_R1/               NMS 1.20.6
v1_21_R1/               NMS 1.21.1
v26_1_R1/               NMS 26.1.2
mythicskywars-assembly/       Shaded build module
build.bat               Build + deploy to server
deploy-to-nexus.ps1     Upload BuildTools artifacts to Nexus
```

---

## 📜 License

This project originates from MythicSkywars. All original code remains under its respective license. New modifications and additions are maintained independently by the Mythical team.
