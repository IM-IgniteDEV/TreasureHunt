# Treasure Hunt Plugin

Create interactive treasure hunts with custom rewards. Players can discover hidden treasures throughout the world, with
each treasure being claimable only once per player.

## Features

- **Multi-Server Support**: Uses MySQL to synchronize found treasures across multiple servers
- **Rich Text Formatting**: Supports MiniMessage formatting for all messages and GUI elements
- **Interactive GUI**: Manage treasures through an intuitive graphical interface
- **Custom Commands**: Execute any command when a treasure is found, with %player% placeholder
- **Efficient**: Uses HikariCP connection pooling for optimal database performance
- **Player Tracking**: Keep track of which players have found which treasures
- **Teleportation**: Instantly teleport to any treasure location from the Admin GUI

## Commands

| Command                           | Description                                 | Permission           |
|-----------------------------------|---------------------------------------------|----------------------|
| `/treasure create <id> <command>` | Create a new treasure at the clicked block  | `treasurehunt.admin` |
| `/treasure delete <id>`           | Delete an existing treasure                 | `treasurehunt.admin` |
| `/treasure completed <id>`        | List players who found a specific treasure  | `treasurehunt.admin` |
| `/treasure list`                  | List all existing treasures                 | `treasurehunt.admin` |
| `/treasure gui`                   | Open the treasure management GUI            | `treasurehunt.admin` |

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

- `treasurehunt.admin` - Grants access to all admin commands
- `treasurehunt.use` - Allows players to find treasures (granted by default)