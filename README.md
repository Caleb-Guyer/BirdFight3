# Bird Fight 3

A chaotic JavaFX multiplayer platformer with epic bird battles, power-ups, AI opponents, and custom maps like forests, cities, and cliffs. Built for fun fights and unlocks—flap to victory!

## Overview

Bird Fight 3 is a feature-rich platform fighter that supports local play, AI opponents, LAN matches, unlockables, and multiple single-player modes. Battle as various bird characters with unique abilities, collect power-ups, and compete in tournaments or tower defense challenges.

## Features

- **Multiple Game Modes**
  - Local multiplayer battles
  - AI-powered opponents with varying difficulty
  - LAN multiplayer matches for network play
  - Tower Defense mode
  - Tournament setup and play
  - Single-player campaign progression

- **Bird Characters & Abilities**
  - Unique playable bird characters (with AI variants like CrowMinion, ChickMinion)
  - Special attacks and movements
  - Character unlocks through progression

- **Dynamic Gameplay**
  - Multiple custom map environments (forests, cities, cliffs, docks)
  - Interactive hazards and environmental elements (Piranha, WindVent, SwingingVine, FrostbiteSnowbank)
  - Power-ups and rewards system
  - Particle effects and visual feedback

- **Input Support**
  - Keyboard and gamepad controls
  - Native Wii Remote support with customizable button mapping
  - Xbox controller support
  - Directional input stabilization and hold detection

- **Progression & Unlocks**
  - Achievement system with categories
  - Bird Coin currency for shop purchases
  - Raritry-based reward packs
  - Match history tracking
  - Profile-based progression

- **Customization**
  - Global settings management
  - Multiple menu themes
  - UI factory for flexible UI generation

## Requirements

- **JDK 21 or newer**
- `JAVA_HOME` environment variable pointing to that JDK
- PowerShell or another shell that can run the Maven wrapper

This project targets Java 21 in `pom.xml`. Maven Enforcer fails fast on older JDKs. If `.\mvnw` fails with `JAVA_HOME not found` or launches under Java 8, point it at a JDK 21+ install first.

## Getting Started

### Build the Project

```bash
./mvnw clean compile
```

### Run the Game

```bash
./mvnw clean javafx:run
```

The game will launch with the main menu. Choose your game mode and start battling!

## Project Structure

```
src/main/java/com/example/birdgame3/
├── Launcher.java                          # Entry point for the application
├── BirdGame3.java                         # Main game controller
├── Bird.java                              # Base bird character class
├── MatchController.java                   # Match game logic
├── MenuLayout.java & MenuTheme.java       # UI framework
├── UIFactory.java                         # Dynamic UI generation
│
├── Game Modes
│   ├── TowerDefenseMode.java
│   ├── BirdGame3TournamentSetupUi.java
│   └── BirdGame3TournamentUi.java
│
├── Characters & Minions
│   ├── ChickMinion.java
│   ├── CrowMinion.java
│   └── various bird AI implementations
│
├── Maps & Environment
│   ├── Platform.java                      # Map platforms
│   ├── PiranhaHazard.java                 # Water hazard
│   ├── WindVent.java                      # Air hazard
│   ├── SwingingVine.java                  # Interactive element
│   ├── FrostbiteSnowbank.java             # Snow hazard
│   ├── DockShipBomb.java                  # Explosive hazard
│   └── NectarNode.java                    # Collectible
│
├── Power-ups & Items
│   ├── PowerUp.java & PowerUpType.java
│   ├── ShopItem.java
│   ├── ShopPackResult.java
│   ├── ShopRarity.java
│   └── PackReward.java
│
├── Input & Controls
│   ├── WiimoteController.java             # Wii Remote support
│   ├── WiimoteInputManager.java
│   ├── WiimoteControlMapper.java
│   ├── XboxInputManager.java              # Xbox controller support
│   └── DirectionalSecretCode.java         # Input pattern detection
│
├── Networking (LAN)
│   ├── LanClient.java
│   ├── LanHostServer.java
│   ├── LanPayloadRouter.java
│   ├── LanProtocol.java
│   └── NetworkSessionClient/Host.java
│
├── Progression & Unlocks
│   ├── BirdGame3AchievementProfile.java   # Achievement tracking
│   ├── BirdGame3AchievementEvaluator.java
│   ├── BirdCoinLedger.java                # Currency system
│   ├── GameSaveRepository.java            # Save data persistence
│   ├── BirdGame3ProfileProgressState.java
│   └── BirdGame3ProgressionService.java
│
└── Utilities
    ├── Particle.java                      # Visual effects
    ├── FrameRateLimiter.java              # Performance control
    ├── DigitalHoldStabilizer.java         # Input smoothing
    └── ThrowableLogSupport.java           # Debugging

```

## Technologies & Dependencies

- **JavaFX 21.0.6** - GUI framework
- **JNA 5.18.1** - Java Native Access for system integration
- **hid4java 0.8.0** - Human Interface Device support (Wii Remote)
- **JUnit 5** - Unit testing framework
- **Maven** - Build automation

## Wii Remote Support

Bird Fight 3 natively supports Wii remotes for enhanced gameplay. Connect your Wii remote and customize button mappings through the settings menu.

## Controls

### Keyboard/Gamepad
- **Movement**: Arrow Keys / Left Stick
- **Jump**: Space / A Button
- **Attack**: Z / X / Y Button
- **Special**: Shift / RT/LT Buttons

### Wii Remote
- Customizable button mapping in settings
- Motion controls available
- See in-game settings for detailed control schemes

## Building & Development

### Prerequisites
- JDK 21+
- Maven 3.6+

### Maven Commands

```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Run the application
./mvnw javafx:run

# Create distribution package
./mvnw package
```

## Troubleshooting

**"JAVA_HOME not found" or Maven launches with Java 8**
- Verify `JAVA_HOME` environment variable points to JDK 21+
- Run: `echo $JAVA_HOME` (Linux/Mac) or `echo %JAVA_HOME%` (Windows)
- Set it explicitly before running Maven

**Wii Remote not detected**
- Ensure hid4java drivers are properly installed
- Check system permissions for USB device access
- Try re-pairing the Wii Remote

**Performance issues**
- Check frame rate limiter settings in global settings
- Reduce particle effects if needed
- Ensure sufficient system resources

## Resources

- **Official Bird Fight 3 Wiki**: https://bird-fight-3.fandom.com/wiki/Bird_Fight_3_Wiki

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

---

**Made with ❤️ and lots of flapping!**
