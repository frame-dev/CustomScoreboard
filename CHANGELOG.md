# 🎯 CustomScoreboard Changelog

## 📦 [1.3-SNAPSHOT] - 2025-04-15

### ✨ Added

- Added new placeholder: `%currency%` for Vault's currency symbol
- Improved scoreboard entry handling with better error recovery
- Enhanced player join event handling for more reliable scoreboard assignment

### 🐛 Fixed

- Fixed scoreboard entry ordering to respect configuration order
- Improved error handling for null player references
- Fixed potential memory leak in scoreboard task management

### 🔄 Changed

- Refactored scoreboard creation logic for better performance
- Improved documentation throughout the codebase
- Enhanced configuration validation

## 📦 [1.2-SNAPSHOT] - 2025-04-14

### ✨ Added

- Protocol version detection using PacketEvents API
- Support for displaying player client version in scoreboard
- Improved error handling for protocol version detection
- Added new placeholders: `%player_version%`, `%time_as_ticks%`, `%player_world_time%`, `%currency%`
- Added new command: `/customscoreboard reload` to reload the plugin configuration
- Only 1.21.4 Server will run otherwise the plugin will be disabled
- Color code support using `&` as a replacement for `§` in all text [List Of Colors](https://minecraft.fandom.com/wiki/Formatting_codes)

### 🐛 Fixed

- Fixed protocol version retrieval method to use correct API calls
- Improved error handling for Vault integration
- Fixed scoreboard update interval configuration
- Fixed regex replacement issue with special characters in replacement values

### 🔄 Changed

- Updated PacketEvents dependency to version 2.7.0
- Improved code organization and documentation
- Enhanced error logging for better debugging
- Improved regex replacement system using Matcher.quoteReplacement for better compatibility

## 📦 [1.1-SNAPSHOT] - 2025-04-10

### ✨ Added

- Vault integration for economy display
- Support for custom date and time formats
- TPS (Ticks Per Second) display
- Experience points display
- Player coordinates display

### 🐛 Fixed

- Scoreboard update mechanism
- Player join event handling
- Configuration file loading

### 🔄 Changed

- Improved scoreboard creation logic
- Enhanced placeholder replacement system
- Better error handling throughout the plugin

## 📦 [1.0-SNAPSHOT] - 2025-04-05

### ✨ Added

- Initial release of CustomScoreboard
- Basic scoreboard functionality
- Support for common placeholders (player name, world, health, food, etc.)
- Configuration system for customizing scoreboard appearance
- Update checker functionality
- Auto-update capability (optional)

### 🎯 Features

- Dynamic scoreboard updates
- Customizable scoreboard lines
- Event-based scoreboard assignment
- Support for multiple scoreboard formats
