### Phase 1: Environment & Parsing Foundation

* [x] Fork or clone the `vstepa/renpy-jetbrains-plugin` prototype.
* [x] Update the `build.gradle.kts` to target the latest IntelliJ Platform SDK.
* [x] Expand the JFlex lexer (`.flex` file) to include all structural keywords (e.g., `transform`, `screen`, `init`, `layeredimage`).
* [x] Implement `LanguageInjector` to inject native Python language support into `python:` blocks and `$` statements for seamless custom logic and UI development.

### Phase 2: Indexing & Code Navigation

* [x] Build a background service that detects and parses `game/saves/navigation.json`.
* [x] Implement `GotoDeclarationHandler` to map Ctrl+Click actions to the file paths and line numbers parsed from the JSON.
* [x] Implement `PsiStructureViewFactory` to scan the current file and populate the IDE's Structure tool window (equivalent to document symbols).

### Phase 3: Editor Enhancements & UX

* [x] Implement `CompletionContributor` to provide context-aware suggestions for labels (e.g., after `jump`), screens (e.g., after `show screen`), and variables.
* [x] Convert the VS Code extension's JSON snippets into JetBrains XML Live Templates for rapid code insertion.
* [x] Implement `DocumentationProvider` to fetch and display docstrings for built-in Ren'Py functions on hover.
* [x] Implement `ElementColorProvider` to read hex color strings and render a clickable native color picker in the editor gutter.

### Phase 4: Diagnostics & Project Tooling

* [x] Implement `ExternalAnnotator` to validate script indentation and basic block structure.
* [x] Add an inspection pass to verify that referenced assets (images, audio files) actually exist within the project's `game/` directory.
* [x] Build a custom Run Configuration (`RunConfigurationExtension`) to launch the project directly via the local Ren'Py SDK executable from within the IDE.

### Phase 5: Deep Code Intelligence & Refactoring

* [ ] Implement PsiReferenceContributor for "Find Usages" (labels, variables)
* [ ] Implement RefactoringSupportProvider for safe variable/label renaming
* [ ] Go to Definition (Ctrl+Click on screens, transforms, labels)
* [ ] Auto-Formatting (FormattingModelBuilder) for smart indent/dedent
* [ ] Extract Label/Screen refactoring action
* [ ] Call Hierarchy tree view (who calls this label, where does it jump)
* [ ] Parameter Info tooltip for screen calls
* [ ] Auto-Import/Include suggestions for missing files
* [ ] Editor Breadcrumbs for file navigation
* [ ] Code Folding for label, screen, and init python blocks

### Phase 6: Writing & Narrative Support

* [ ] Dialogue Word Count (ignoring code and structural syntax)
* [ ] Dialogue-Only Spellcheck (ignoring Python variables)
* [ ] Character Line Counter
* [ ] Dialogue Tree Visualizer (node-based map)
* [ ] Voice Script Exporter (CSV/Excel generation for voice actors)
* [ ] Voice Line Auto-Matcher inspection
* [ ] Sprite Emotion Auto-Complete based on images/ directory scan

### Phase 7: Media & Asset Management

* [ ] Image Hover Preview thumbnails
* [ ] Audio Hover Playback controls
* [ ] Asset Drag-and-Drop code generation
* [ ] Unused Asset Detector
* [ ] Image Resolution Inspector (flags assets larger than game resolution)
* [ ] Missing Asset Quick-Fix (generate placeholder file)
* [ ] Color Palette Extractor from images

### Phase 8: UI & Screen Development

* [ ] Style Property Autocomplete (xalign, ypos, background, etc.)
* [ ] Style Hierarchy Viewer
* [ ] ATL (Animation) Snippet Generator
* [ ] Visual Easing Editor (curve editor pop-up for ATL)
* [ ] Color Contrast Checker for accessibility
* [ ] GUI Variable Linker (gui.text_color jumps to gui.rpy)
* [ ] Live Screen Preview panel

### Phase 9: Advanced Diagnostics & Linting

* [ ] Dead Code Detector (unreachable labels/screens)
* [ ] Infinite Loop Detector (while loops without pauses/menus)
* [ ] Missing Return Statement warning
* [ ] Python 2 vs 3 Syntax Checker (Ren'Py 7 vs Ren'Py 8)
* [ ] Overlapping Asset Names error detection
* [ ] Conflicting Keymap Inspector

### Phase 10: Build, Tooling, & Python Magic

* [ ] Integrated Ren'Py Lint Wrapper UI
* [ ] One-Click Distributions (Mac/Win/Linux/Android)
* [ ] Error Log Parser (clickable traceback.txt file paths)
* [ ] SDK Version Manager (quick-swap Ren'Py engines)
* [ ] Save File Editor (.save inspector and modifier)
* [ ] RPYC Decompiler tool
* [ ] Ren'Py Core API Autocomplete (renpy.show(), renpy.pause(), etc.)
* [ ] Python 3 Standard Library Support inside init python blocks
* [ ] CDD (Creator-Defined Displayable) Python class scaffolding
* [ ] Global Variable Scope Visualizer