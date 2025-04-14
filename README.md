# 🎯 CustomScoreboard

**CustomScoreboard** is a powerful Bukkit/Spigot plugin that provides a fully customizable scoreboard system for Minecraft servers. Display dynamic, real-time data such as player stats, server info, and more — all in the sidebar.

---

## Changelogs

[Changelogs](https://github.com/frame-dev/CustomScoreboard/blob/master/CHANGELOG.md)

---

## Status

[![Build Java with Maven](https://github.com/frame-dev/CustomScoreboard/actions/workflows/maven.yml/badge.svg)](https://github.com/frame-dev/CustomScoreboard/actions/workflows/maven.yml)

---

## Caution: This plugin is in **early development** and may contain bugs

---

## Required Depdencencies

- [PacketEvents](https://www.spigotmc.org/resources/packetevents-api.80279/)

## ✨ Features

- 🎯 Dynamic placeholders for player and server data
- 💰 Vault integration for economy display
- ⚙️ Fully customizable scoreboard lines via `config.yml`
- 🔄 Automatic updates for scoreboard values
- 📦 Event-based scoreboard assignment per player
- Color code support using `&` as a replacement for `§` in all text [List Of Colors](https://minecraft.fandom.com/wiki/Formatting_codes)

---

## 📦 Installation

1. Download the plugin `.jar` file.
2. Place it in your server's `/plugins` directory.
3. Restart or reload the server.

---

## ⚙️ Configuration

Edit the `config.yml` file to customize your scoreboard.  
Here's an example:

``` yaml
scoreboard:
  "player":
    name: "&a&lPlayer&r"
    value: "%player%"
  "ping":
    name: "&a&lPing&r"
    value: "%ping%"
  "world":
    name: "&a&lWorld&r"
    value: "%world%"
  "coordinates":
    name: "&a&lCoordinates&r"
    value: "%coordinates%"
  "level":
    name: "&a&lLevel&r"
    value: "%level%"
  "player-time":
    name: "&a&lPlayer Time&r"
    value: "%player_world_time%"
  "money":
    name: "&a&lMoney&r"
    value: "%money%"
  "server-name":
    name: "&a&lServerName&r"
    value: "&6Your Server Name"
  "tps":
    name: "&a&lTPS&r"
    value: "%tps%"
  "player-version":
    name: "&a&lPlayer Version&r"
    value: "%player_version%"
  "online-players":
    name: "&a&lOnline Players&r"
    value: "%online% / %max_players%"

```

---

## 🧩 Supported Placeholders

| Placeholder        | Description                         |
|--------------------|-------------------------------------|
| `%player%`         | Player's name                       |
| `%world%`          | Player's current world              |
| `%time%`           | Current server time                 |
| `%date%`           | Current server date                 |
| `%online%`         | Online player count                 |
| `%max_players%`    | Maximum number of players           |
| `%money%`          | Player's balance (requires Vault)   |
| `%ping%`           | Player's ping                       |
| `%coordinates%`    | Player's XYZ coordinates            |
| `%health%`         | Player's current health             |
| `%food%`           | Player's food level                 |
| `%level%`          | Player's XP level                   |
| `%ip%`             | Player's IP address                 |
| `%version%`        | Server version                      |
| `%player_version%` | Player's client version (e.g., 1.21.4) |
| `%tps%`            | Server's TPS (Ticks Per Second)     |
| `%exp%` | Player's current experience points to next level in decimal like 0.95 from 1.0 (1.0 means level up)|
| `%time_as_ticks%`   | Worlds Time as Ticks               |
| `%player_world_time%` | Player's current World Time      |

---

## 🧪 Development

### 📋 Prerequisites

- Java 17 or higher
- Maven

### 🔧 Build

Use the following command to compile the plugin:

``` shell
mvn clean package
```

The compiled `.jar` will be located in the `target/` directory.

---

## 📄 License

This project is licensed under the **GNU V3 License**.  
See the LICENSE file for details.
