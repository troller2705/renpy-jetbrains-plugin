package ee.vstepa.jetbrains.plugins.renpy.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.element.type.RenPyScriptElementTypes
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenTypes
import ee.vstepa.jetbrains.plugins.renpy.service.RenPyNavigationService

class RenPyAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {

        // -------------------------------------------------------------
        // 1. Indentation Validation
        // -------------------------------------------------------------
        // Ren'Py technically accepts any indentation, but strongly encourages
        // multiples of 4 spaces. Uneven indentation is the #1 cause of visual novel bugs.
        if (element.node.elementType == RenPyScriptTokenTypes.INDENT) {
            val indentLength = element.textLength
            if (indentLength % 4 != 0) {
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Ren'Py standard indentation is a multiple of 4 spaces (Found $indentLength spaces).")
                    .range(element)
                    .create()
            }
        }

        // -------------------------------------------------------------
        // 2. Structural & Reference Validation
        // -------------------------------------------------------------
        // Use our Navigation Service from Phase 2 to verify if jump/call targets actually exist!
        if (element.node.elementType == RenPyScriptElementTypes.JUMP_STMT_TARGET ||
            element.node.elementType == RenPyScriptElementTypes.CALL_STMT_TARGET) {

            val targetName = element.text
            val navService = element.project.service<RenPyNavigationService>()

            // If the label doesn't exist anywhere in the project cache, flag it as a hard error.
            if (!navService.getAllLabels().contains(targetName)) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved label: '$targetName'")
                    .range(element)
                    // This makes the text red and squiggly, just like an unresolved variable in Python/Java
                    .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                    .create()
            }
        }
    }
}