package ee.vstepa.jetbrains.plugins.renpy.color

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ElementColorProvider
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenTypes
import java.awt.Color

class RenPyColorProvider : ElementColorProvider {

    companion object {
        // Cache the context so we NEVER have to touch the invalidated PsiElement
        // that JetBrains accidentally passes to us during rapid mouse dragging.
        private var activeMarker: RangeMarker? = null
        private var activeProject: Project? = null
        private var activeDocument: Document? = null
    }

    override fun getColorFrom(element: PsiElement): Color? {
        if (element.node.elementType != RenPyScriptTokenTypes.STRING) return null

        val text = element.text
        if ((text.startsWith("\"#") && text.endsWith("\"")) || (text.startsWith("'#") && text.endsWith("'"))) {
            val hex = text.trim('"', '\'').trim()

            try {
                return when (hex.length) {
                    4 -> { // #RGB
                        val r = "${hex[1]}${hex[1]}".toInt(16)
                        val g = "${hex[2]}${hex[2]}".toInt(16)
                        val b = "${hex[3]}${hex[3]}".toInt(16)
                        Color(r, g, b)
                    }
                    5 -> { // #RGBA
                        val r = "${hex[1]}${hex[1]}".toInt(16)
                        val g = "${hex[2]}${hex[2]}".toInt(16)
                        val b = "${hex[3]}${hex[3]}".toInt(16)
                        val a = "${hex[4]}${hex[4]}".toInt(16)
                        Color(r, g, b, a)
                    }
                    7 -> { // #RRGGBB
                        Color.decode(hex)
                    }
                    9 -> { // #RRGGBBAA
                        val r = hex.substring(1, 3).toInt(16)
                        val g = hex.substring(3, 5).toInt(16)
                        val b = hex.substring(5, 7).toInt(16)
                        val a = hex.substring(7, 9).toInt(16)
                        Color(r, g, b, a)
                    }
                    else -> null
                }
            } catch (e: NumberFormatException) {
                return null
            }
        }
        return null
    }

    override fun setColorTo(element: PsiElement, color: Color) {
        // 1. Is this a fresh click? If so, the element is valid. Cache the environment!
        if (element.isValid) {
            activeProject = element.project
            activeDocument = PsiDocumentManager.getInstance(activeProject!!).getDocument(element.containingFile)
            activeMarker = activeDocument?.createRangeMarker(element.textRange)
        }

        // 2. Retrieve our safely cached environment.
        // We do NOT touch 'element' beyond this point!
        val project = activeProject ?: return
        val document = activeDocument ?: return
        val marker = activeMarker ?: return

        if (!marker.isValid) return

        val start = marker.startOffset
        val end = marker.endOffset
        val currentText = document.getText(TextRange(start, end))

        // 3. Format the new color string
        val quote = if (currentText.startsWith("'")) "'" else "\""
        val rawHex = currentText.trim('"', '\'').trim()

        val newHexString = if (rawHex.length == 5 || rawHex.length == 9 || color.alpha != 255) {
            String.format("%s#%02x%02x%02x%02x%s", quote, color.red, color.green, color.blue, color.alpha, quote)
        } else {
            String.format("%s#%02x%02x%02x%s", quote, color.red, color.green, color.blue, quote)
        }

        // 4. Update the text document and realign the marker
        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, newHexString)

            // Re-anchor the tracker for the next frame of the mouse drag
            activeMarker = document.createRangeMarker(start, start + newHexString.length)

            // Force IDE to sync the text immediately
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }
}