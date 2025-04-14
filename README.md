# 🎯 CustomScoreboard

**CustomScoreboard** is a powerful Bukkit/Spigot plugin that provides a fully customizable scoreboard system for Minecraft servers. Display dynamic, real-time data such as player stats, server info, and more — all in the sidebar.

---

## Caution: This plugin is in **early development** and may contain bugs

---

## ✨ Features

- 🎯 Dynamic placeholders for player and server data
- 💰 Vault integration for economy display
- ⚙️ Fully customizable scoreboard lines via `config.yml`
- 🔄 Automatic updates for scoreboard values
- 📦 Event-based scoreboard assignment per player

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
      displayName: "&6Scoreboard"
      updateInterval: 20
      1:
        name: "&ePlayer"
        value: "&7%player%"
      2:
        name: "&eWorld"
        value: "&7%world%"
      3:
        name: "&eTime"
        value: "&7%time%"
      4:
        name: "&eDate"
        value: "&7%date%"
      5:
        name: "&eIP"
        value: "&7%ip%"
      6:
        name: "&eVersion"
        value: "&7%version%"
      7:
        name: "&eYour Version"
        value: "&7%player_version%"
      8:
        name: "&eOnline"
        value: "&7%online%/%max_players%"
      9:
        name: "&eServer"
        value: "&7%server_name%"
      10:
        name: "&eMoney"
        value: "&7%money%"
      11:
        name: "&ePing"
        value: "&7%ping%"
      12:
        name: "&eLocation"
        value: "&7%coordinates%"
      13:
        name: "&eHealth"
        value: "&7%health%"
      14:
        name: "&eFood"
        value: "&7%food%"
      15:
        name: "&eLevel"
        value: "&7%level%"
      16:
        name: "&eTPS"
        value: "&7%tps%"
      17:
        name: "&eExperience"
        value: "&7%exp%"
    formats:
      date: "yyyy-MM-dd"
      time: "HH:mm:ss"
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
| `%time_as_ticks%`   | Worlds Time as Ticks                |

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
