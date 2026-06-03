package ee.vstepa.jetbrains.plugins.renpy.run

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import com.intellij.openapi.ui.TextBrowseFolderListener

class RenPySettingsEditor : SettingsEditor<RenPyRunConfiguration>() {
    private val sdkPathField = TextFieldWithBrowseButton()
    private val mainPanel: JPanel

    init {
        // 1. Configure the descriptor with our title and description
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            .withTitle("Select Ren'Py SDK Executable")
            .withDescription("Choose the renpy.exe (Windows) or renpy.sh (Mac/Linux) file")

        // 2. Attach it using the modern TextBrowseFolderListener
        sdkPathField.addBrowseFolderListener(TextBrowseFolderListener(descriptor))

        // Build the UI panel
        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Ren'Py SDK Executable Path:", sdkPathField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    // Load saved settings into the UI
    override fun resetEditorFrom(config: RenPyRunConfiguration) {
        sdkPathField.text = config.sdkPath
    }

    // Save UI settings back to the configuration
    override fun applyEditorTo(config: RenPyRunConfiguration) {
        config.sdkPath = sdkPathField.text
    }

    override fun createEditor(): JComponent = mainPanel
}