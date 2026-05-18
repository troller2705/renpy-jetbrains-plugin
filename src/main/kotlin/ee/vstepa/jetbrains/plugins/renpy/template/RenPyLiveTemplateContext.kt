package ee.vstepa.jetbrains.plugins.renpy.template

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import ee.vstepa.jetbrains.plugins.renpy.lang.RenPyScriptLanguage

class RenPyLiveTemplateContext : TemplateContextType("RENPY", "Ren'Py") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        // Only allow these templates to trigger inside actual Ren'Py files
        return templateActionContext.file.language.isKindOf(RenPyScriptLanguage.INSTANCE)
    }
}