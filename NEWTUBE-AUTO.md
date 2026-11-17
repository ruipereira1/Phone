# NewTube Auto

Fork of [NewTube](https://github.com/aleixrodriala/newtube) (GPL-3.0) with Android Auto screens.
Based on upstream commit `6f96a1b`.

## What it adds
- **Car screens** (`smarttubetv/src/stmobile/java/com/newtube/mobile/auto/`): Subscriptions, Home,
  History, Search and a player screen, built with the Car App Library (`androidx.car.app`).
- **Own player for the car** (`AutoPlayer`): the phone's `Media3PlayerInitializer` and
  `Media3PlayerController`, so the car uses the same network stack; a media session for the
  steering-wheel buttons.
- **Video only when parked** (`DrivingState`, `AutoSurfaceRenderer`): the app declares the
  navigation category, which gives it a drawing surface. Video is drawn there only while the car's
  speed (CAR_SPEED) or the phone's GPS says the car is stopped; otherwise the video track is
  switched off and only the sound plays.
- `-PautoBuild` gives the app id `io.github.aleixrodriala.arc.auto` (installs next to NewTube) and
  turns the official in-app updater off. The app is named "NewTube Auto".

## Install
1. Install `NewTube-Auto.apk` from the `newtube-auto` release and sign in on the phone.
2. Android Auto on the phone: Settings, tap "Version" 10 times, then ⋮ > Developer settings >
   **Unknown sources** on.
3. Give NewTube Auto location access (the parked/moving check).
