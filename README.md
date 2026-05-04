# TextDisplayModeler

TextDisplayModeler is a Minecraft Paper plugin that allows you to load and render 3D models (STL & OBJ formats) directly in-game using Minecraft's Text Display entities. Powered by the [TextDisplayShapes](https://github.com/TWME-TW/TextDisplayShapes) API, it provides an efficient and creative way to visualize 3D objects in your Minecraft world.

## Features

- **3D Model Loading**: Load standard `.stl` and `.obj` files directly from the plugin's `models` folder.
- **Render Modes**: Choose between `bukkit` mode (standard Bukkit API) or `packet` mode (using PacketEvents for client-side rendering to save server resources).
- **Customizable Appearance**: Dynamically scale models, set maximum view distances, and apply custom hex colors (`#FFFFFF`).
- **Interactive UI**: Manage your models with an easy-to-use, clickable MiniMessage-based chat interface.

## Requirements

- **Minecraft Version**: 1.21 or higher (Paper API)
- **Java Version**: 21
- **Dependencies**:
  - [PacketEvents](https://github.com/retrooper/packetevents) (Required)

## Installation

1. Download the latest `TextDisplayModeler.jar` from the releases page (or build it yourself).
2. Download and install [PacketEvents](https://github.com/retrooper/packetevents) into your `plugins` folder.
3. Place `TextDisplayModeler.jar` into your server's `plugins` folder.
4. Start the server to generate the default configuration and folders.
5. Place your `.stl` or `.obj` model files into the `plugins/TextDisplayModeler/models/` directory.

## Commands & Permissions

**Base Command**: `/textdisplaymodeler` or `/tsmodel`  
**Permission**: `textdisplaymodeler.use` (Grants access to all commands)

| Command | Description |
|---|---|
| `/tsmodel list [page]` | Lists all available models in the `models/` directory. |
| `/tsmodel load <filename>` | Loads a model file into memory. (e.g., `/tsmodel load my_model.stl`) |
| `/tsmodel spawn <name> <mode> [scale] [viewDistance] [color]` | Spawns a loaded model in the world at your current location. |
| `/tsmodel unload <name>` | Unloads a model from memory to free up resources. |
| `/tsmodel removeall` | Removes all active spawned model instances. |
| `/tsmodel debug` | Spawns a debug triangle to test if the underlying rendering engine is working. |

### Spawn Command Arguments

- `<name>`: The name of the loaded model (filename without extension).
- `<mode>`: The rendering mode (`bukkit` or `packet`).
- `[scale]`: Optional. The size multiplier for the model (Default: `1.0`).
- `[viewDistance]`: Optional. How far away the model can be seen in blocks (Default: `64.0`).
- `[color]`: Optional. The hex color code for the model (Default: `FFFFFF`).

**Example Usage**:  
`/tsmodel spawn cube packet 2.5 128 FF0000`  
*(Spawns a model named "cube" using packet mode, scaled 2.5x, visible up to 128 blocks away, colored Red).*

## Building from Source

To compile the project yourself, ensure you have Java 21 and Maven installed.

```bash
git clone <your-repo-url>
cd TextDisplayModeler
mvn clean package
```

The compiled plugin will be located in the `target/` directory.
