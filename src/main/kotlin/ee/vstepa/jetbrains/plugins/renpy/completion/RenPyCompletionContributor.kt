package ee.vstepa.jetbrains.plugins.renpy.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenTypes
import ee.vstepa.jetbrains.plugins.renpy.service.RenPyNavigationService

class RenPyCompletionContributor : CompletionContributor() {

    init {
        // ---------------------------------------------------------
        // PROVIDER 1: Inline Python Variables (Hooks into your custom Lexer Token!)
        // ---------------------------------------------------------
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(RenPyScriptTokenTypes.ONE_LINE_PYTHON_STATEMENT),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    val navService = parameters.position.project.service<RenPyNavigationService>()

                    // 1. Get the exact text the user has typed so far inside the python token
                    val currentText = parameters.position.text
                    val caretOffset = parameters.offset - parameters.position.textRange.startOffset
                    val textBeforeCaret = currentText.substring(0, caretOffset)

                    // 2. Strip out the dollar sign and grab just the current word (e.g., "$ my_v" -> "my_v")
                    val prefix = textBeforeCaret.substringAfterLast(" ").replace("$", "").trim()

                    // 3. Force JetBrains to use our clean prefix for the search filter
                    val cleanResultSet = resultSet.withPrefixMatcher(prefix)

                    for (variable in navService.getAllVariables()) {
                        cleanResultSet.addElement(
                            LookupElementBuilder.create(variable)
                                .withIcon(AllIcons.Nodes.Variable)
                                .withTypeText("Variable")
                        )
                    }
                }
            }
        )

        // ---------------------------------------------------------
        // PROVIDER 2: Standard Ren'Py Keywords, Labels, and Screens
        // ---------------------------------------------------------
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    // Ignore this provider if we are inside a python line (Provider 1 handles that)
                    if (parameters.position.node.elementType == RenPyScriptTokenTypes.ONE_LINE_PYTHON_STATEMENT) return

                    val element = parameters.position
                    val document = parameters.editor.document
                    val lineStart = document.getLineStartOffset(document.getLineNumber(parameters.offset))
                    val textBeforeCaret = document.getText(TextRange(lineStart, parameters.offset))
                    val navService = element.project.service<RenPyNavigationService>()

                    // Suggest Labels
                    if (textBeforeCaret.matches(Regex(""".*\b(jump|call)\s+[\w]*"""))) {
                        for (label in navService.getAllLabels()) {
                            resultSet.addElement(LookupElementBuilder.create(label).withIcon(AllIcons.Nodes.Tag).withTypeText("Label"))
                        }
                        return
                    }

                    // Suggest Screens
                    if (textBeforeCaret.matches(Regex(""".*\b(show screen|hide screen|call screen)\s+[\w]*"""))) {
                        for (screen in navService.getAllScreens()) {
                            resultSet.addElement(LookupElementBuilder.create(screen).withIcon(AllIcons.Nodes.Desktop).withTypeText("Screen"))
                        }
                        return
                    }

                    // Suggest standard keywords and variables (for if/while blocks)
                    val keywords = listOf(
                        "label", "menu", "jump", "call", "return", "pass",
                        "if", "elif", "else", "while",
                        "show", "scene", "hide", "with",
                        "play", "stop", "queue", "voice",
                        "define", "default", "image", "transform", "screen", "style", "init python:"
                    )

                    if (textBeforeCaret.matches(Regex(""".*\s*[\w]*"""))) {
                        for (keyword in keywords) resultSet.addElement(LookupElementBuilder.create(keyword).withBoldness(true).withTypeText("Keyword"))
                        for (variable in navService.getAllVariables()) resultSet.addElement(LookupElementBuilder.create(variable).withIcon(AllIcons.Nodes.Variable).withTypeText("Variable"))
                    }
                }
            }
        )
    }

    // Force the popup to open instantly without needing Ctrl+Space
    override fun invokeAutoPopup(position: com.intellij.psi.PsiElement, typeChar: Char): Boolean {
        if (typeChar == '$' || typeChar == '_' || typeChar.isLetterOrDigit()) {
            return true
        }
        return super.invokeAutoPopup(position, typeChar)
    }
}