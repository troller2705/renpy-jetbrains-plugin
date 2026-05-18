### Phase 1: Environment & Parsing Foundation

* [x] Fork or clone the `vstepa/renpy-jetbrains-plugin` prototype.
* [ ] Update the `build.gradle.kts` to target the latest IntelliJ Platform SDK.
* [ ] Expand the JFlex lexer (`.flex` file) to include all structural keywords (e.g., `transform`, `screen`, `init`, `layeredimage`).
* [ ] Implement `LanguageInjector` to inject native Python language support into `python:` blocks and `$` statements for seamless custom logic and UI development.

### Phase 2: Indexing & Code Navigation

* [ ] Build a background service that detects and parses `game/saves/navigation.json`.
* [ ] Implement `GotoDeclarationHandler` to map Ctrl+Click actions to the file paths and line numbers parsed from the JSON.
* [ ] Implement `PsiStructureViewFactory` to scan the current file and populate the IDE's Structure tool window (equivalent to document symbols).

### Phase 3: Editor Enhancements & UX

* [ ] Implement `CompletionContributor` to provide context-aware suggestions for labels (e.g., after `jump`), screens (e.g., after `show screen`), and variables.
* [ ] Convert the VS Code extension's JSON snippets into JetBrains XML Live Templates for rapid code insertion.
* [ ] Implement `DocumentationProvider` to fetch and display docstrings for built-in Ren'Py functions on hover.
* [ ] Implement `ElementColorProvider` to read hex color strings and render a clickable native color picker in the editor gutter.

### Phase 4: Diagnostics & Project Tooling

* [ ] Implement `ExternalAnnotator` to validate script indentation and basic block structure.
* [ ] Add an inspection pass to verify that referenced assets (images, audio files) actually exist within the project's `game/` directory.
* [ ] Build a custom Run Configuration (`RunConfigurationExtension`) to launch the project directly via the local Ren'Py SDK executable from within the IDE.
