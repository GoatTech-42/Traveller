# Traveller

A complete teleportation suite for **Purpur / Paper** servers. Traveller bundles
spawn, homes, TPA and back into one lightweight plugin with configurable warmups,
cooldowns, sounds, particles and a live countdown.

- **Version:** 1.0.0
- **Minecraft:** 1.21.11
- **Tested against:** Paper 26.1.2 (Purpur is a drop-in replacement for Paper)
- **Java:** 21

## Features

| Feature | Commands |
| ------- | -------- |
| Spawn   | `/spawn`, `/setspawn` |
| Homes   | `/home [name]`, `/sethome [name]`, `/delhome <name>`, `/homes` |
| TPA     | `/tpa <player>`, `/tpahere <player>`, `/tpaccept [player]`, `/tpdeny [player]`, `/tpcancel` |
| Back    | `/back` |
| Admin   | `/traveller <reload\|info\|version>` |

Each feature can be turned on or off in `config.yml`, and warmups / cooldowns can
be set globally or per-feature.

## Installation

1. Download `Traveller-1.0.0.jar` from the [Releases](../../releases) page.
2. Drop it into your server's `plugins/` folder.
3. Start (or restart) the server to generate `config.yml`.
4. Edit `config.yml` to taste and run `/traveller reload`.

## Building from source

You'll need JDK 21 and Maven.

```bash
mvn clean package
```

The finished plugin lands at `target/Traveller-1.0.0.jar`.

## Configuration

The full configuration with comments lives in
[`src/main/resources/config.yml`](src/main/resources/config.yml). A few highlights:

- **Warmups** – seconds the player must stand still before teleporting. The
  countdown is shown on the action bar, a title, or in chat.
- **Cooldowns** – seconds before the same command can be used again.
- **cancel-on-move / cancel-on-damage** – break the warmup if the player moves
  or takes damage.
- **Sounds** – use the modern key form (`block.note_block.hat`); the old enum
  names (`BLOCK_NOTE_BLOCK_HAT`) still work too.

## Permissions

| Permission | Default | Description |
| ---------- | ------- | ----------- |
| `traveller.spawn` | everyone | Use `/spawn` |
| `traveller.setspawn` | op | Use `/setspawn` |
| `traveller.home` / `sethome` / `delhome` / `homes` | everyone | Home commands |
| `traveller.tpa` / `tpahere` / `tpaccept` / `tpdeny` | everyone | TPA commands |
| `traveller.back` | everyone | Use `/back` |
| `traveller.admin` | op | Reload and info |
| `traveller.homes.<n>` | – | Allow up to `<n>` homes |
| `traveller.homes.unlimited` | op | No home limit |
| `traveller.bypass.warmup` | op | Skip warmups |
| `traveller.bypass.cooldown` | op | Skip cooldowns |

## License

Released under the MIT License. See [LICENSE](LICENSE).
