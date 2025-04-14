# 🎯 CustomScoreboard

**CustomScoreboard** is a powerful Bukkit/Spigot plugin that provides a fully customizable scoreboard system for Minecraft servers. Display dynamic, real-time data such as player stats, server info, and more — all in the sidebar.

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
Here’s an example:

    scoreboard:
      displayName: "&6My Custom Scoreboard"
      updateInterval: 20
      0:
        name: "&aPlayer:"
        value: "%player%"
      1:
        name: "&bWorld:"
        value: "%world%"
      2:
        name: "&cOnline Players:"
        value: "%online%"
    formats:
      time: "HH:mm:ss"
      date: "yyyy-MM-dd"

---

## 🧩 Supported Placeholders

| Placeholder        | Description                         |
|--------------------|-------------------------------------|
| %player%           | Player’s name                       |
| %world%            | Player’s current world              |
| %time%             | Current server time                 |
| %date%             | Current server date                 |
| %online%           | Online player count                 |
| %max_players%      | Maximum number of players           |
| %server_name%      | Server name                         |
| %money%            | Player’s balance (requires Vault)   |
| %ping%             | Player’s ping                       |
| %coordinates%      | Player’s XYZ coordinates            |
| %health%           | Player’s current health             |
| %food%             | Player’s food level                 |
| %level%            | Player’s XP level                   |

---

## 🛠️ Commands

| Command                  | Description                        |
|--------------------------|------------------------------------|
| /customscoreboard reload | Reloads the plugin configuration   |

---

## 🔐 Permissions

| Permission                | Description                      |
|---------------------------|----------------------------------|
| customscoreboard.admin    | Allows access to admin commands  |

---

## 🧪 Development

### 📋 Prerequisites

- Java 17 or higher
- Maven

### 🔧 Build

Use the following command to compile the plugin:

    mvn clean package

The compiled `.jar` will be located in the `target/` directory.

---

## 📄 License

This project is licensed under the **GNU V3 License**.  
See the LICENSE file for details.
