### Modifications

- Preset files now save render & OpenSCAD paths, as well as *Keep .scad files* setting
- Fixed an issue where keytops would not fit on the corresponding key stalks
- Added two additional parameters for fine-tuning (see below)

### Launching

Create a start batch file with a call such as

    java --module-path <Path to OpenJFX>\openjfx-17.0.12_windows-x64_bin-sdk\lib  --add-modules javafx.controls,javafx.fxml -jar <Path to Repo>\OverKeys-Generator-Mod\dist\OverKeys.jar

### New parameters in modified version

- *extra lever length* (in mm, default: 0.0) - extends distance between rod and last key row *(useful for mechanical pianos, example values: 10.0, 20.00)* 
- *prow height factor* (0.0 to 1.0, default: 0.5) - the bigger, the shorter the height of the pointy end of "long" black keys *(useful to avoid overlay keys hitting white keys on uneven keybeds, example values: 0.6, 0.8)*

### Janko keyboard settings

- *Half-steps to Period*: 2
- *Half-steps to Generator*: 1
- *Half-steps to large MOS-step*: 1
- *Gamut (notes/period)*: 5 or 6 (nr of Janko rows)
- *Range*: 12 
- *Starting Key*: 0-->A, 1-->Bb, etc.
- *Pointy-up hexagons*
- *ShiftY*: something like 0.6494251777385843
- *keytop height diff*: 0.25 or higher

### Other recommendation

- for me, a *Stalk fit X tolerance* of 0.1 produced tightly fitting keytops (default: 0.3)

### Tipps for OpenSCAD beginners

- always render with F6 first before scrolling around large models (much faster!)
- use the latest *nightly* version, which supports measuring distances
- in *preferences*, feature *fast-csg* should be activated for this to work
