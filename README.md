# Treasure Hunt Plugin

Create interactive treasure hunts with custom rewards. Players can discover hidden treasures throughout the world, with each treasure being claimable only once per player.

## Features

- **Multi-Server Support**: Uses MySQL to synchronize found treasures across multiple servers
- **Custom Commands**: Execute any command when a treasure is found, with player placeholders
- **Admin Tools**: Easy management of treasures through commands and GUI
- **Efficient**: Uses connection pooling for optimal database performance
- **Player Tracking**: Keep track of which players have found which treasures

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/treasure create <id> <command>` | Create a new treasure at the clicked block | `treasurehunt.admin` |
| `/treasure delete <id>` | Delete an existing treasure | `treasurehunt.admin` |
| `/treasure completed <id>` | List players who found a specific treasure | `treasurehunt.admin` |
| `/treasure list` | List all existing treasures | `treasurehunt.admin` |
| `/treasure gui` | Open the treasure management GUI (optional) | `treasurehunt.admin` |


## Configuration

Example `config.yml`:

```yaml
# Database Configuration
mysql:
  host: localhost
  port: 3306
  database: minecraft
  username: user
  password: password
  table-prefix: treasurehunt_
  
# Plugin Settings
settings:
  # How often to save data to the database (in seconds)
  save-interval: 300
  
  # Message when a player finds a treasure
  found-message: "&aYou found a treasure!"
```

## Permissions

- `treasurehunt.admin` - Grants access to all admin commands
- `treasurehunt.use` - Allows players to find treasures (granted by default)

## Usage

1. **Creating a Treasure**:
   ```
   /treasure create reward1 say %player% found the first treasure!
   ```
   Then click the block you want to set as treasure.

2. **Deleting a Treasure**:
   ```
   /treasure delete reward1
   ```

3. **Checking Treasure Completions**:
   ```
   /treasure completed reward1
   ```