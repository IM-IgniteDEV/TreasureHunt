![Treasure Hunt Banner](https://i.imgur.com/01BQpvE.png)

# Treasure Hunt
*Turn your server into a world full of hidden riches!*

## Overview
Create interactive treasure hunts with custom rewards! Players can discover hidden treasures scattered throughout your world, with each treasure being claimable only once per player.

## Features
- **Multi-Server Support** — synchronize treasure data via MySQL across servers
- **Rich Text Formatting** — full MiniMessage support for all plugin messages and GUI elements
- **Interactive GUI** — manage treasures through an intuitive admin interface
- **Custom Commands** — run any command when a treasure is found (supports %player% placeholder)
- **Efficient** — optimized with HikariCP connection pooling
- **Player Tracking** — keep track of which players have found which treasures
- **Teleportation** — instantly teleport to treasures directly from the admin GUI

## Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/treasure create <id> <command>` | Create a new treasure at the clicked block | treasurehunt.admin |
| `/treasure delete <id>` | Delete an existing treasure | treasurehunt.admin |
| `/treasure completed <id>` | List players who found a specific treasure | treasurehunt.admin |
| `/treasure list` | List all existing treasures | treasurehunt.admin |
| `/treasure gui` | Open the treasure management GUI | treasurehunt.admin |

## Configuration
```yaml
# Database Configuration
mysql:
  host: localhost
  port: 3306
  database: minecraft
  username: user
  password: password
  table-prefix: treasurehunt_

# Plugin Messages (supports MiniMessage format)
messages:
  prefix: "<gradient:gold:yellow>TreasureHunt</gradient> <white>»"
  alreadyFound: "<red>You have already found this treasure"
  found: "<green>You found a treasure!"
  noPermission: "<red>You don't have permission to use this command"
  playerOnly: "<red>This command can only be used by players"
```

## Permissions
- **treasurehunt.admin** — Grants access to all admin commands  
- **treasurehunt.use** — Allows players to find treasures

---
**Ready to fill your world with secrets?**  
Download now and start your Treasure Hunt adventure!
