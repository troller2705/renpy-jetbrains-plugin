package ee.vstepa.jetbrains.plugins.renpy.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement

class RenPyDocumentationProvider : AbstractDocumentationProvider() {

    // A mini-database of Ren'Py documentation.
    // You can easily expand this later or even load it from a JSON file!
    private val docDatabase = mapOf(
        "label" to "<b>label</b> <i>name</i><br/><br/>Declares a label. Labels are used to define points in the story that can be jumped to or called.",
        "menu" to "<b>menu</b><br/><br/>Presents the user with a list of choices. The choices are defined by strings, followed by a colon and a block of Ren'Py statements.",
        "jump" to "<b>jump</b> <i>label</i><br/><br/>Transfers control to the given label. The call stack is cleared, meaning you cannot return to where you jumped from.",
        "call" to "<b>call</b> <i>label</i><br/><br/>Transfers control to the given label. When a <code>return</code> statement is reached, control returns to the statement following the call.",
        "show" to "<b>show</b> <i>image_name</i> [<i>at transform</i>]<br/><br/>Displays an image on a layer. Does not cause an interaction.",
        "scene" to "<b>scene</b> <i>image_name</i><br/><br/>Clears the current layer and then displays an image on it.",
        "transform" to "<b>transform</b> <i>name</i><br/><br/>Defines a transform, which can be applied to an image to change its position, size, or other properties."
    )

    // 1. Intercept the hover action
    override fun getCustomDocumentationElement(
        editor: com.intellij.openapi.editor.Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        // If they are hovering over a raw keyword that exists in our database, grab it!
        if (contextElement is LeafPsiElement && docDatabase.containsKey(contextElement.text.trim())) {
            return contextElement
        }
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset)
    }

    // 2. Generate the HTML popup
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val text = element?.text?.trim() ?: return null
        val docText = docDatabase[text] ?: return null

        // Wrap it in standard IDE HTML styling
        return "<div class='content'>$docText</div>"
    }
}