# Depth Wallpaper (Nothing Phone 1 / any Android 12+ device)

A live wallpaper app that mimics iOS's depth-effect wallpaper: pick a photo,
the app cuts the subject out, and the live wallpaper draws the subject crisp
in front of a blurred copy of the same photo. The background shifts more
than the subject as you swipe home screens or tilt the phone, which is what
sells the illusion of depth.

## What's included
- `MainActivity` — photo picker + preview + "Set as Wallpaper" button
- `SegmentationHelper` — runs ML Kit's on-device selfie segmentation to cut
  the subject out with a transparent background
- `WallpaperStore` — saves the two layers (foreground cutout, background) to
  app-internal storage
- `DepthWallpaperService` — a `WallpaperService` that draws both layers each
  frame, blurring the background live with `RenderEffect` and offsetting each
  layer by a different amount based on `onOffsetsChanged` (home screen swipe)
  and the rotation vector sensor (tilt)

## Get an installable APK without Android Studio (recommended if you just want to install it)

This repo includes a GitHub Actions workflow that builds the APK for you on
GitHub's servers — you never touch Gradle or an SDK.

1. Create a free GitHub account if you don't have one (github.com).
2. Create a new **public or private repo** (any name, e.g. `depth-wallpaper`).
3. Upload every file/folder from this zip into that repo (on github.com you
   can drag-and-drop the extracted folder contents into the repo's "Add
   file > Upload files" screen — do this from a computer, not the phone).
4. Commit the upload. This triggers the **Build Debug APK** workflow
   automatically (you can also trigger it manually from the repo's
   **Actions** tab > "Build Debug APK" > "Run workflow").
5. Wait ~2–4 minutes for the run to finish (green checkmark).
6. Open that workflow run, scroll to **Artifacts**, and download
   `DepthWallpaper-debug-apk` — it's a zip containing `app-debug.apk`.
7. Transfer `app-debug.apk` to your Nothing Phone (email it to yourself,
   Google Drive, USB — any method).
8. On the phone, tap the APK file. Android will prompt to allow installs
   from that source (Settings > install unknown apps) — approve it, then
   install. This is a normal prompt for any APK not from the Play Store,
   not a red flag specific to this app.
9. Open the app, pick a photo, then **Set as Wallpaper**.

## Requirements
- Android Studio (Koala or newer)
- A device or emulator on Android 12 (API 31)+ — Nothing Phone 1 shipped on
  Android 12 and is fine
- No API keys or network access needed; segmentation runs fully on-device

## Build & install
1. Open the `DepthWallpaper` folder in Android Studio and let it sync Gradle.
2. Run the `app` module on your phone (USB debugging on, or build a signed
   APK from **Build > Generate Signed Bundle/APK**).
3. Open the app, tap **Choose Photo**, pick a portrait-style photo with a
   clear subject (person works best — see limitations below).
4. Once processing finishes, tap **Set as Wallpaper**. Android's live
   wallpaper picker opens with Depth Wallpaper preselected — confirm to set
   it as your home screen background.

## Known limitations / good next steps
- **Segmentation model**: `segmentation-selfie` is tuned for people. For
  pets, objects, or scenery, swap it for ML Kit's Subject Segmentation API
  (currently beta) or let the user brush in a mask manually.
- **Edge quality**: the mask cutoff is a hard 0.5 confidence threshold. For
  softer edges, feather the mask (blend alpha near the boundary) instead of
  a binary cut.
- **Multiple wallpapers / presets**: currently only one photo is stored at a
  time. A `RecyclerView` of saved presets plus per-preset files in
  `WallpaperStore` would let users switch without re-picking a photo.
- **Battery**: sensor-driven redraws on every `onSensorChanged` tick can add
  up. Consider throttling with a small `Handler` delay if you notice drain.
- **Lock screen**: this only covers the home screen live wallpaper surface;
  Android's lock screen depth effect (Android 14+) uses a separate API
  (`WallpaperManager.setWallpaperOffsetSteps` variants aside, real subject-
  aware lock screen wallpaper is a system feature you can't hook into from a
  third-party app).

## How the depth illusion actually works
Both layers are the *same* photo. The background layer is a blurred, ~12%
zoomed-in copy that moves up to ~60px with swipe/tilt; the foreground layer
is the segmented subject (transparent elsewhere) that only moves ~12px.
Because near objects appear to move less than distant ones as your viewpoint
shifts (real-world parallax), giving the subject a smaller travel range than
the background reproduces that same visual cue.
