package ee.vstepa.jetbrains.plugins.renpy.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.element.type.RenPyScriptElementTypes
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenTypes
import java.io.File

class RenPyAssetInspection : LocalInspectionTool() {

    // Common media extensions used in Ren'Py
    private val imageExtensions = listOf(".png", ".jpg", ".jpeg", ".webp", ".avif", ".svg")
    private val audioExtensions = listOf(".ogg", ".mp3", ".wav", ".opus", ".flac")
    private val videoExtensions = listOf(".webm", ".mp4", ".ogv", ".mkv", ".avi")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                // 1. Audio Control Statements (e.g., play music "bgm" or "audio/bgm.ogg")
                if (element.node.elementType == RenPyScriptElementTypes.AUDIO_CONTROL_STMT_AUDIO_FILE) {
                    val rawText = element.text
                    if (rawText.startsWith("\"") || rawText.startsWith("'")) {
                        val relativePath = rawText.trim('"', '\'')
                        validateAssetWithOptionalExtensions(element, relativePath, holder, "Audio", audioExtensions)
                    }
                }

                // 2. Generic Strings with explicit media extensions
                // Covers images, audio variables, and video files (e.g., add "movies/intro.webm")
                if (element.node.elementType == RenPyScriptTokenTypes.STRING) {
                    // Prevent double-checking if this string was already caught by the audio checker above
                    if (element.parent?.node?.elementType == RenPyScriptElementTypes.AUDIO_CONTROL_STMT_AUDIO_FILE) return

                    val cleanString = element.text.trim('"', '\'')
                    val lowerString = cleanString.lowercase()

                    if (imageExtensions.any { lowerString.endsWith(it) }) {
                        validateAssetWithOptionalExtensions(element, cleanString, holder, "Image", listOf(""))
                    } else if (audioExtensions.any { lowerString.endsWith(it) }) {
                        validateAssetWithOptionalExtensions(element, cleanString, holder, "Audio", listOf(""))
                    } else if (videoExtensions.any { lowerString.endsWith(it) }) {
                        // NEW: Trigger validation for video files
                        validateAssetWithOptionalExtensions(element, cleanString, holder, "Video", listOf(""))
                    }
                }
            }
        }
    }

    private fun validateAssetWithOptionalExtensions(
        element: PsiElement,
        relativePath: String,
        holder: ProblemsHolder,
        assetType: String,
        allowedExtensions: List<String>
    ) {
        val projectBasePath = element.project.basePath ?: return

        val extensionsToTry = if (allowedExtensions.any { relativePath.lowercase().endsWith(it) }) {
            listOf("")
        } else {
            listOf("") + allowedExtensions
        }

        var fileExists = false

        for (ext in extensionsToTry) {
            val testPath = relativePath + ext
            val possibleFiles = listOf(
                File(projectBasePath, "game/$testPath"),
                File(projectBasePath, testPath),
                File(projectBasePath, "game/images/$testPath"),
                File(projectBasePath, "game/audio/$testPath"),
                File(projectBasePath, "game/gui/$testPath"),
                File(projectBasePath, "game/movies/$testPath"),
                File(projectBasePath, "game/video/$testPath")
            )

            if (possibleFiles.any { it.exists() && it.isFile }) {
                fileExists = true
                break
            }
        }

        if (!fileExists) {
            holder.registerProblem(
                element,
                "Unresolved $assetType asset: '$relativePath'. File does not exist in the project."
            )
        }
    }
}