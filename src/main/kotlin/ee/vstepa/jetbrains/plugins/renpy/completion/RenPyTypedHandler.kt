package ee.vstepa.jetbrains.plugins.renpy.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import ee.vstepa.jetbrains.plugins.renpy.lang.RenPyScriptLanguage

class RenPyTypedHandler : TypedHandlerDelegate() {

    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        // 1. Make sure we are only messing with Ren'Py files
        if (file.language !is RenPyScriptLanguage) {
            return Result.CONTINUE
        }

        // 2. If they type a '$', or any letter/number, forcefully summon the popup
        if (charTyped == '$' || charTyped.isLetterOrDigit() || charTyped == '_') {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor)

            // If it's specifically the dollar sign, we tell the IDE to STOP processing
            // other default punctuation rules that might try to close the menu.
            if (charTyped == '$') {
                return Result.STOP
            }
        }

        // 3. Otherwise, let the IDE handle the keystroke normally (like spaces or enters)
        return Result.CONTINUE
    }
}