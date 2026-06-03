package ee.vstepa.jetbrains.plugins.renpy.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import org.jdom.Element

class RenPyRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<RunConfigurationOptions>(project, factory, name) {

    var sdkPath: String = ""

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return RenPySettingsEditor()
    }

    // This is where the magic happens when you click the Green Play Button
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            override fun startProcess(): ProcessHandler {
                val commandLine = GeneralCommandLine()

                if (sdkPath.isBlank()) {
                    throw RuntimeException("Ren'Py SDK path is not configured. Please edit your Run Configuration.")
                }

                // 1. Point to the Ren'Py SDK executable
                commandLine.exePath = sdkPath

                // 2. Pass your current project directory as the argument so Ren'Py launches your game directly
                val projectPath = environment.project.basePath ?: throw RuntimeException("Project path not found.")
                commandLine.addParameter(projectPath)

                // 3. Spawn the terminal process inside the JetBrains Run Tool Window
                val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }
        }
    }

    // Safely save the path to the disk so it remembers your SDK location between restarts
    override fun readExternal(element: Element) {
        super.readExternal(element)
        sdkPath = JDOMExternalizerUtil.readField(element, "sdkPath", "")
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, "sdkPath", sdkPath)
    }
}