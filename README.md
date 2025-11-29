# PortalToggler

> A simple plugin to control Nether and End portal entry and creation on Paper/Spigot.

## Description

PortalToggler lets server admins easily enable or disable Nether/End portal entry and creation. It's lightweight, easy to configure, and great for events, minigames, or any server that needs tight portal control.

## Features

- Toggle Nether/End entry and creation separately
- Bypass permissions for trusted players
- Fully customizable MiniMessage messages
- Reload config without restarting
- Built-in debug mode
- Tab completion for all commands
- Lightweight and simple to set up

## Usage

### Commands

<> = required, [] = optional

| Command | Description | Permission |
|---------|-------------|------------|
| `/portal` or `/portaltoggler` | Main plugin command | `portaltoggler.admin` |
| `/portal help` | Display command help | `portaltoggler.admin` |
| `/portal reload` | Reload configuration files | `portaltoggler.admin` |
| `/portal toggle <nether\|end> <entry\|creation>` | Toggle portal state | `portaltoggler.admin` |
| `/portal status [nether\|end]` | Check portal status (all or specific) | `portaltoggler.admin` |
| `/portal enable <nether\|end> <entry\|creation>` | Enable specific portal function | `portaltoggler.admin` |
| `/portal disable <nether\|end> <entry\|creation>` | Disable specific portal function | `portaltoggler.admin` |

## Configuration

### config.yml

The main configuration file controls portal states and debug settings:

```yaml
# PortalToggler Configuration

# Debug Settings
debug: false

# Portal Settings
portals:
  nether:
    entry:
      enabled: true
    creation:
      enabled: true
  end:
    entry:
      enabled: true
    creation:
      enabled: true
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `debug` | boolean | `false` | Enable debug logging for troubleshooting |
| `portals.nether.entry.enabled` | boolean | `true` | Allow players to enter nether portals |
| `portals.nether.creation.enabled` | boolean | `true` | Allow players to create nether portals |
| `portals.end.entry.enabled` | boolean | `true` | Allow players to enter end portals |
| `portals.end.creation.enabled` | boolean | `true` | Allow players to create end portals (place eyes of ender) |

### messages.yml

Customize all plugin messages with MiniMessage formatting:

```yaml
prefix: "<dark_gray>[</dark_gray><gradient:#ff55ff:#5555ff>PortalToggler</gradient><dark_gray>]</dark_gray>"

# Status Messages
status-enabled: "<green>Enabled</green>"
status-disabled: "<red>Disabled</red>"

# Portal Messages
portal-disabled: "<red>Disabled {portal} portal {mode}."
portal-enabled: "<green>Enabled {portal} portal {mode}."

disabled-portal-attempt: "<red>You cannot enter the {portal} as portal entry is disabled."
disabled-end-portal-creation: "<red>You cannot insert an eye of ender into an {portal} portal frame while it is disabled."
disabled-nether-portal-creation: "<red>You cannot create a {portal} portal while it is disabled."

# Command Messages
no-permission: "<red>You don't have permission to use this command."
reload-success: "<green>Configuration reloaded successfully!"
```

**MiniMessage Formatting:**
- Supports gradients, colors, hover text, and more
- Learn more at [MiniMessage Documentation](https://docs.advntr.dev/minimessage/format.html)

### Command Examples

```
# Toggle nether portal entry
/portal toggle nether entry

# Disable end portal creation
/portal disable end creation

# Check all portal statuses
/portal status

# Check only nether portal status
/portal status nether

# Reload configuration
/portal reload
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `portaltoggler.admin` | Access to all plugin commands | op |
| `portaltoggler.bypass.enter.nether` | Bypass nether portal entry restrictions | op |
| `portaltoggler.bypass.enter.end` | Bypass end portal entry restrictions | op |
| `portaltoggler.bypass.create.nether` | Bypass nether portal creation restrictions | op |
| `portaltoggler.bypass.create.end` | Bypass end portal creation restrictions | op |

### Permission Examples

**LuckPerms:**
```
# Grant admin access to a group
/lp group admin permission set portaltoggler.admin true

# Allow VIP players to bypass nether portal restrictions
/lp group vip permission set portaltoggler.bypass.enter.nether true
/lp group vip permission set portaltoggler.bypass.create.nether true
```

**PermissionsEx:**
```
/pex group admin add portaltoggler.admin
/pex group vip add portaltoggler.bypass.enter.nether
```

## Building from Source

### Prerequisites

- Java Development Kit (JDK) 21 or higher
- Maven 3.x
- Git

### Build Steps

1. Clone the repository:
```bash
git clone https://github.com/chokedd/PortalToggler.git
cd PortalToggler
```

2. Build with Maven:
```bash
mvn clean package
```

3. The compiled JAR will be located in the `target` directory as `PortalToggler-1.0.jar`

### Development Setup

1. Import the project into your IDE as a Maven project
2. Ensure your IDE is configured to use Java 21
3. The project uses the Paper API 1.21.4 (automatically downloaded by Maven)

## Use Cases

- **Event Servers:** Temporarily disable portal access during special events
- **Minigame Servers:** Control portal functionality for specific game modes
- **Survival Servers:** Prevent nether/end access until certain conditions are met
- **Creative Servers:** Disable portal creation to prevent unwanted structures
- **Hub Servers:** Completely disable portals in lobby areas

## Contributing

Contributions are welcome! If you'd like to contribute to PortalToggler:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate documentation.

## Support

If you encounter any issues or have questions:

1. Check the [Issues](https://github.com/chokedd/PortalToggler/issues) page
2. Create a new issue with detailed information about your problem
3. Include your server version, plugin version, and any error messages

## License

PortalToggler is licensed under the MIT License. See [LICENSE](./LICENSE) for details.