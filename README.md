# LiteVotePlugin

A small Paper plugin that lets players vote to change the time of day or the weather,
instead of having an admin do it or waiting for someone to sleep.

Any player starts a vote with `/vote <target>`. Everyone else has 30 seconds to answer
with `/vote yes` or `/vote no`. If there are more yes votes than no votes when the timer
runs out, the change is applied to the world the vote was started in. Only one vote can
be running at a time, and each player gets one vote.

## Commands

| Command | What it does |
| --- | --- |
| `/vote day` | Set time to morning |
| `/vote night` | Set time to night |
| `/vote clear` | Stop the rain |
| `/vote rain` | Start the rain |
| `/vote storm` | Start a thunderstorm |
| `/vote yes` / `/vote no` | Answer the running vote |

The initiator's vote counts as yes automatically. A reminder is broadcast 15 seconds
before the vote closes.

## Requirements

- Paper 1.21.11
- Java 21

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`. Drop it into your server's `plugins/` folder.

To try it out without a separate server, `./gradlew runServer` spins up a Paper server
in `run/` with the plugin already loaded.
