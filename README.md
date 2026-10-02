# Minecraft pre-Classic rd-132211

Recreation of **Minecraft Java Edition pre-Classic rd-132211** (May 13, 2009, 20:11 UTC) — the earliest Minecraft version ever available.

The `rd` prefix stands for **RubyDung**, the early-2009 game whose codebase Notch repurposed for Minecraft.

## Requirements

- Java 8 or newer
- Maven 3.6+
- A GPU/driver that supports OpenGL (LWJGL 2)

## Build

```bash
mvn clean package
```

This produces a fat JAR at `target/rd-132211.jar` with LWJGL natives for Windows, Linux, and macOS bundled inside.

To refresh the Windows launcher at the repo root (requires .NET Framework `csc`):

```powershell
csc /target:winexe /r:System.Windows.Forms.dll /out:rd-132211.exe launcher\Launcher.cs
copy target\rd-132211.jar rd-132211.jar
```

## Run

**Windows (easiest):** double-click `rd-132211.exe` in the project root  
(needs `rd-132211.jar` beside it and Java 8+ installed)

Or from the JAR:

```bash
java -jar rd-132211.jar
# or
java -jar target/rd-132211.jar
```

Natives are extracted to a temp directory automatically on startup. `level.dat` is created in the **current working directory** (the folder you launch from).

## Controls

| Input | Action |
|-------|--------|
| W / A / S / D (or arrows) | Move |
| Space | Jump |
| Mouse | Look |
| Left click | Place block |
| Right click | Destroy block |
| Enter / Numpad Enter | Save level to `level.dat` |
| R | Respawn at a random location |
| Esc | Quit (also saves) |

## Authentic behavior & intentional bugs

- **Flat world** 256×64×256; solid terrain Y=0..42 (surface grass at Y=42), air above. Build limit is Y=64 (world depth).
- **Stone and grass share block ID 1**; grass is visual-only on the surface layer.
- **Camera pivot bug**: view rotates then translates, shifting perspective while looking around.
- **Place inside yourself**: no occupancy check when placing (fixed later in Classic 0.0.9a).
- **GL_SELECT picking**: every nearby block face is re-rendered into the selection buffer each frame (can tank framerate on Intel GPUs).
- **Flashing white overlay** on the targeted block face.
- **`level.dat`**: raw gzipped block bytes, no metadata. Any gzipped file named `level.dat` can be loaded without validation (smaller files keep generated padding; larger are truncated).
