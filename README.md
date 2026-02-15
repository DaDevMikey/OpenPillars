# OpenPillars

[![Build](https://github.com/DaDevMikey/OpenPillars/actions/workflows/build.yml/badge.svg)](https://github.com/DaDevMikey/OpenPillars/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8--1.21%2B-brightgreen)](https://www.spigotmc.org/)

A high-performance, cross-version **Pillars of Fortune** engine for Minecraft servers. Built with modern design principles, full customization, and an API-first approach for developers.

---

## Features

### No Cages — Seamless Start
Unlike traditional pillar games, OpenPillars uses a **freeze mechanic** instead of cages. Players spawn on a 3×3 platform atop their pillar and can look around freely but can't move until the countdown ends.

### Cross-Version Support
One JAR to rule them all. Using [XSeries](https://github.com/CryptoMorin/XSeries) for material and sound mapping, OpenPillars works seamlessly from **1.8 to 1.21+** without any version-specific builds.

### Performance-First
- **Async Configuration Loading** — Config files load asynchronously to maintain TPS
- **Async Block Generation** — Pillar generation is scheduled off the main thread
- **Concurrent Collections** — Thread-safe data structures for player management
- **No TPS Impact** — Designed to keep your server at a solid 20.0 TPS

### Pillar Mechanics
- Players start on a 3×3 stone platform so they have room to stand
- Pillars grow 1×1 from the center, with new blocks appearing above the platform
- Players are nudged upward when a block generates beneath them — no getting stuck
- Loot drops directly into the player's inventory when they break a pillar block

### Fully Customizable Loot System
```yaml
loot-tables:
  standard-pillar:
    - material: "DIAMOND_SWORD"
      weight: 5
      amount: 1
      name: "&bExcalibur"
      lore:
        - "&7A legendary blade"
      enchantments:
        - "SHARPNESS:2"
```

- **Weighted Random** — Higher weight = more common drops
- **Custom Names & Lore** — Full color code and hex color support
- **Enchantments** — Add any enchantment at any level
- **Variable Amounts** — Use ranges like `"1-5"` for variable drops
- **Dynamic Loot Tables** — Switch loot tables mid-game based on time

### Lobby / Hub Teleport
After a game ends (or when a player leaves), they are automatically teleported back to:
1. A configured lobby location (if set in `config.yml`)
2. The main world's spawn point (fallback — works great for hub servers)

### Developer API
OpenPillars fires custom events for other plugins to hook into:

```java
@EventHandler
public void onPillarBreak(PillarBlockBreakEvent event) {
    Player player = event.getPlayer();
    ItemStack loot = event.getLoot();

    // Modify loot, cancel the event, etc.
    event.setLoot(new ItemStack(Material.DIAMOND, 64));
}
```

**Available Events:**
| Event | Description |
|---|---|
| `GameStartEvent` | When a game begins |
| `GameEndEvent` | When a game ends (includes winner) |
| `GameStateChangeEvent` | State machine transitions |
| `PillarBlockGenerateEvent` | Before a block is placed on a pillar |
| `PillarBlockBreakEvent` | When a player breaks a pillar block |
| `PlayerEliminatedEvent` | When a player is eliminated |

### PlaceholderAPI Support
Use these placeholders on scoreboards, holograms, or anywhere:

| Placeholder | Description |
|---|---|
| `%openpillars_state%` | Current game state |
| `%openpillars_players%` | Player count |
| `%openpillars_players_alive%` | Alive player count |
| `%openpillars_kills%` | Player's kill count |
| `%openpillars_blocks_broken%` | Blocks broken by player |
| `%openpillars_time%` | Game time (MM:SS) |
| `%openpillars_in_game%` | Is player in game (`true`/`false`) |

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/pillars join` | `openpillars.command.join` | Join a game |
| `/pillars leave` | `openpillars.command.leave` | Leave the current game |
| `/pillars start` | `openpillars.command.start` | Force start the game |
| `/pillars stop` | `openpillars.command.stop` | Stop the current game |
| `/pillars reload` | `openpillars.command.reload` | Reload configuration |
| `/pillars setup` | `openpillars.command.setup` | Setup arena |
| `/pillars help` | — | Show help message |

**Aliases:** `/op`, `/pillar`, `/pf`

---

## Installation

1. Download the latest release from [Releases](https://github.com/DaDevMikey/OpenPillars/releases)
2. Place in your server's `plugins/` folder
3. Restart your server
4. Edit config files in `plugins/OpenPillars/`
5. Use `/pillars reload` to apply changes

## Configuration

| File | Purpose |
|---|---|
| `config.yml` | Game mechanics, timings, world/arena settings, sounds, lobby location |
| `loot.yml` | Loot tables with weighted items, custom names, enchantments |
| `messages.yml` | Fully customizable messages with color codes, hex colors, and placeholders |

### World & Arena Options
You can either set explicit center coordinates for the arena or use **offset-from-spawn** mode to auto-place the arena far from your lobby — all configurable in `config.yml`.

---

## Building from Source

```bash
git clone https://github.com/DaDevMikey/OpenPillars.git
cd OpenPillars
mvn clean package
```

The shaded JAR will be at `target/OpenPillars-1.0.0.jar`.

> **Note:** Requires JDK 8+ to build. The output JAR targets Java 8 for maximum server compatibility.

## Requirements

- **Java 8+** (runtime)
- **Spigot / Paper 1.8 — 1.21+**
- **PlaceholderAPI** *(optional, for placeholders)*

---

## Contributing

Contributions are welcome! Please feel free to submit pull requests.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

## Support

- **Issues:** [GitHub Issues](https://github.com/DaDevMikey/OpenPillars/issues)

---

Made with ❤️ for the Minecraft community
