package ee.vstepa.jetbrains.plugins.renpy.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

class RenPyRunConfigurationType : ConfigurationTypeBase(
    "RenPyRunConfiguration",
    "Ren'Py Game",
    "Run configuration to launch a Ren'Py Visual Novel",
    // Uses the native JetBrains console/run icon
    NotNullLazyValue.createValue { AllIcons.Nodes.Console }
) {
    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun createTemplateConfiguration(project: Project): RunConfiguration {
                return RenPyRunConfiguration(project, this, "Ren'Py Game")
            }
            override fun getId(): String = "RenPyRunConfigurationFactory"
        })
    }
}