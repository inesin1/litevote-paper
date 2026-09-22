# LiteVotePlugin

[![CI Build & Test](https://github.com/inesin1/litevote-paper/actions/workflows/ci.yml/badge.svg)](https://github.com/inesin1/litevote-paper/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/inesin1/litevote-paper?include_prereleases&color=brightgreen)](https://github.com/inesin1/litevote-paper/releases)
[![Paper 1.21](https://img.shields.io/badge/Paper-1.21%2B-blue)](https://papermc.io)
[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/technologies/downloads/#java21)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A lightweight, modern, and highly customizable **Paper 1.21+** plugin that allows players to vote for changing the time of day or weather — without needing admin intervention or sleeping through the night.

---

## ✨ Features

- 🖱️ **Interactive Chat:** Clickable `[ЗА]` / `[ПРОТИВ]` buttons in chat with hover tooltips powered by Adventure MiniMessage.
- 🔊 **Sound Effects:** Audible cues on vote start, reminders, success, and failure.
- ⏱️ **Flexible Timers & Reminders:** Configurable vote duration, ending countdown reminder, and cooldown between votes.
- 📊 **Customizable Vote Threshold:** Choose between **Simple Majority** (`yes > no`) or **Player Percentage** (e.g. 50% of active players).
- 🌍 **World-Specific or Global:** Restrict voting and weather changes to the initiator's current world or apply them server-wide.
- 🛡️ **Permissions & Admin Controls:** Configurable permission nodes and an in-game config reload command (`/vote reload`).
- ⚡ **Modern Paper API:** Built on the latest Paper Brigadier command lifecycle and MiniMessage formatting.

---

## 🎮 Commands & Aliases

Commands can be run using either `/vote` or `/v`.

| Command | Permission | Description |
| --- | --- | --- |
| `/vote day` | `litevote.vote` | Start a vote to set time to day |
| `/vote night` | `litevote.vote` | Start a vote to set time to night |
| `/vote clear` | `litevote.vote` | Start a vote for clear weather |
| `/vote rain` | `litevote.vote` | Start a vote for rain |
| `/vote storm` | `litevote.vote` | Start a vote for a thunderstorm |
| `/vote yes` | `litevote.vote` | Vote in favor of the active proposal |
| `/vote no` | `litevote.vote` | Vote against the active proposal |
| `/vote reload` | `litevote.admin` | Reload `config.yml` without server restart |
| `/vote` | `litevote.vote` | View available commands and help |

---

## 🔑 Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `litevote.vote` | `true` (all players) | Allows players to initiate and participate in votes |
| `litevote.admin` | `op` | Allows reloading the configuration via `/vote reload` |
| `litevote.bypass.cooldown` | `op` | Allows starting a vote ignoring the cooldown timer |

---

## ⚙️ Configuration (`config.yml`)

The plugin generates a fully documented `config.yml` upon first run:

```yaml
settings:
  # Duration of the vote in seconds
  vote-duration: 30

  # Seconds before the vote ends to send a reminder broadcast (0 to disable)
  reminder-seconds: 15

  # Cooldown between votes in seconds (0 to disable)
  cooldown-seconds: 60

  # Cooldown mode:
  # - "global": Nobody can start a vote until cooldown expires
  # - "player": Only the vote initiator has a cooldown
  cooldown-mode: "global"

  # Vote requirement mode:
  # - "simple_majority": YES votes must be strictly greater than NO votes (yes > no)
  # - "percentage": Percentage of eligible players who voted YES must reach required-percentage
  requirement-mode: "simple_majority"

  # Required percentage of YES votes if requirement-mode is "percentage" (1 - 100)
  required-percentage: 50

  # If true, votes and effects are restricted to the initiator's world
  world-only: true

  # Minimum number of online players required to start a vote
  min-players: 1

# Sound effects (Minecraft sound keys)
sounds:
  enabled: true
  vote-start: "block.note_block.bell"
  vote-reminder: "block.note_block.pling"
  vote-success: "entity.player.levelup"
  vote-failure: "entity.villager.no"
  vote-cast: "ui.button.click"

# Localized names for vote targets
targets:
  day: "День"
  night: "Ночь"
  clear: "Ясно"
  rain: "Дождь"
  storm: "Гроза"

# Messages using Adventure MiniMessage format (colors, gradients, click & hover events)
messages:
  prefix: "<gray>[<green>LiteVote</green>]</gray> "
  vote-start: "<green><player></green> начал голосование за <gold><target></gold>!\n<gray>Голосуйте: <click:run_command:'/vote yes'><hover:show_text:'<green>Нажмите, чтобы проголосовать ЗА'><green><bold>[ЗА]</bold></green></hover></click> или <click:run_command:'/vote no'><hover:show_text:'<red>Нажмите, чтобы проголосовать ПРОТИВ'><red><bold>[ПРОТИВ]</bold></red></hover></click> <dark_gray>(<duration> сек.)</dark_gray></gray>"
  vote-reminder: "<gray>До конца голосования осталось <yellow><seconds_left></yellow> сек.!</gray>"
  vote-success: "<green>Голосование завершено успешно! (<green><yes_votes></green> ЗА / <red><no_votes></red> ПРОТИВ). Применяем: <gold><target></gold>!</green>"
  vote-failure: "<red>Голосование не прошло! (<green><yes_votes></green> ЗА / <red><no_votes></red> ПРОТИВ). Запрос отклонён.</red>"
  # ... and more customizable strings
```

---

## 📦 Requirements & Installation

- **Server Software:** Paper, Purpur, or compatible 1.21+ fork
- **Java Version:** Java 21 or higher

### Installation:
1. Download the latest `LiteVotePlugin-x.x.x.jar` from [Releases](https://github.com/inesin1/litevote-paper/releases).
2. Place the `.jar` file into your server's `plugins/` directory.
3. Restart or start your server.
4. Edit `plugins/LiteVotePlugin/config.yml` if you want custom settings or translations, then run `/vote reload`.

---

## 🛠️ Building from Source

```bash
# Clone repository
git clone https://github.com/inesin1/litevote-paper.git
cd litevote-paper

# Build and run tests
./gradlew build

# Run local Paper development test server
./gradlew runServer
```

The compiled plugin jar will be generated in `build/libs/LiteVotePlugin-1.1.0.jar`.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
