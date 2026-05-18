package ee.vstepa.jetbrains.plugins.renpy.lang.script.injection

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.jetbrains.python.PythonLanguage
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenTypes

class RenPyPythonLanguageInjector : MultiHostInjector {

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        // 1. Safety check: Ensure the element is legally allowed to host injected code
        if (context !is PsiLanguageInjectionHost) return

        val tokenType = context.node.elementType

        // 2. Handle one-line Python statements (e.g., $ money += 10)
        if (tokenType == RenPyScriptTokenTypes.ONE_LINE_PYTHON_STATEMENT) {
            val text = context.text
            if (text.startsWith("$")) {
                // Skip the '$' sign (and the space after it if it exists)
                val prefixLength = if (text.startsWith("$ ")) 2 else 1

                // Only inject if there is actually python code typed after the $
                if (text.length > prefixLength) {
                    val pythonTextRange = TextRange(prefixLength, text.length)
                    registrar.startInjecting(PythonLanguage.getInstance())
                        // We safely pass context because we verified it is a PsiLanguageInjectionHost above
                        .addPlace(null, null, context, pythonTextRange)
                        .doneInjecting()
                }
            }
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        // Tell the IDE to only run this injector on legal host elements
        return listOf(PsiLanguageInjectionHost::class.java)
    }
}