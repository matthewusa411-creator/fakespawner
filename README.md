# Fake Spawner (Fabric 1.21.11, client only)

Makes blast furnaces act like skeleton spawners on your client:

- Placing a blast furnace registers it as a fake spawner (stack of 1); mining it removes it.
- Right click opens a spawner-style screen ("N SKELETON SPAWNERS"). Bones and arrows pile up on their own.
  Sell All and Drop Loot only reset the fake piles.
- Crouch + right click a fake spawner while holding blast furnaces: "You added N Spawners to the stack,
  There is now T in the stack." The items vanish on your client only and come back when you rejoin.
- Blast furnaces use the Mob Spawner place, break, step and hit sounds, and give off smoke and flame particles.
- All state lives in client memory and is cleared when you disconnect.

## Build

Needs JDK 21 and Gradle 8.14+ with internet access.

    gradle build

The jar is `build/libs/fakespawner-1.0.0.jar`. Put it in `mods/` with Fabric Loader and Fabric API.

Before building, check `gradle.properties` against https://fabricmc.net/develop/ for 1.21.11
(loader, Fabric API and Loom versions are guesses).

## Not included

The spinning skeleton and cage inside the block, and all the cheat modules (sound ESP, storage ESP,
freecam, freelook, suspicious chunks).
