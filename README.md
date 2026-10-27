# Ray (Fabric 1.21.11)

Features: FPS, CPS, ping, coordinates, direction, clock, RAM, keystrokes (WASD/mouse/space),
armor + durability HUD, zoom (hold C), toggle sprint, in-game settings menu (Right Shift).
Config is saved to `.minecraft/config/lumina.json`.

## Build
Requires JDK 21.
1. Open this folder in IntelliJ IDEA (it will import Gradle automatically), then run the `build` task
   OR from a terminal with Gradle 9.2+ installed:
       gradle wrapper
       ./gradlew build        (Windows: gradlew.bat build)
2. Your jar is in build/libs/lumina-client-1.0.0.jar  (NOT the -sources jar)

## Install
Put the jar and Fabric API (1.21.11) in .minecraft/mods with Fabric Loader 0.18+.
