package ee.vstepa.jetbrains.plugins.renpy.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import ee.vstepa.jetbrains.plugins.renpy.service.RenPyNavigationService

class RenPyProjectStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Grab our service and tell it to parse the JSON file
        project.service<RenPyNavigationService>().initIndex()
    }
}