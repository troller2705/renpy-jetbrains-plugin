package ee.vstepa.jetbrains.plugins.renpy.navigation

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.element.type.RenPyScriptElementTypes
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenTypes

class RenPyStructureViewElement(private val myElement: PsiElement) : StructureViewTreeElement {

    override fun getValue(): Any = myElement

    override fun navigate(requestFocus: Boolean) {
        if (myElement is NavigatablePsiElement) {
            myElement.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean = myElement is NavigatablePsiElement && myElement.canNavigate()

    override fun canNavigateToSource(): Boolean = myElement is NavigatablePsiElement && myElement.canNavigateToSource()

    override fun getPresentation(): ItemPresentation {
        // 1. Root File Presentation
        if (myElement is PsiFile) {
            return PresentationData(myElement.name, null, null, null)
        }

        val type = myElement.node.elementType

        // 2. Standard Label Presentation
        if (type == RenPyScriptElementTypes.LABEL_STMT) {
            val nameNode = myElement.node.findChildByType(RenPyScriptElementTypes.LABEL_STMT_NAME)
            val labelName = nameNode?.text ?: "label"
            return PresentationData(labelName, "label", null, null)
        }

        // 3. Standard Menu Presentation
        if (type == RenPyScriptElementTypes.MENU_STMT) {
            val nameNode = myElement.node.findChildByType(RenPyScriptElementTypes.MENU_STMT_NAME)
            val menuName = nameNode?.text?.let { " $it" } ?: ""
            return PresentationData("menu$menuName", "menu", null, null)
        }

        // 4. Custom Declaration Blocks (screens, transforms, defines, etc.)
        if (type == RenPyScriptElementTypes.GEN_STMT_KEYWORD) {
            val keywordToken = myElement.firstChild?.node?.elementType
            val targetTokens = setOf(
                RenPyScriptTokenTypes.SCREEN_KEYWORD,
                RenPyScriptTokenTypes.TRANSFORM_KEYWORD,
                RenPyScriptTokenTypes.DEFINE_KEYWORD,
                RenPyScriptTokenTypes.DEFAULT_KEYWORD,
                RenPyScriptTokenTypes.INIT_KEYWORD,
                RenPyScriptTokenTypes.PYTHON_KEYWORD,
                RenPyScriptTokenTypes.STYLE_KEYWORD
            )

            if (targetTokens.contains(keywordToken)) {
                var name = ""
                // Scan the text directly to the right of the keyword to get the variable/screen name
                var sibling = myElement.nextSibling
                while (sibling != null) {
                    val siblingType = sibling.node.elementType
                    // Stop reading if we hit the end of the line or the colon opening the block
                    if (siblingType == RenPyScriptTokenTypes.NEW_LINE || siblingType == RenPyScriptElementTypes.GEN_STMT_COLON) {
                        break
                    }
                    if (sibling.text.isNotBlank()) {
                        name += sibling.text + " "
                    }
                    sibling = sibling.nextSibling
                }

                val keywordText = myElement.text.trim()
                val displayName = if (name.isBlank()) keywordText else "$keywordText ${name.trim()}"
                return PresentationData(displayName, keywordText, null, null)
            }
        }

        // Fallback
        return PresentationData(myElement.text.take(20) + "...", null, null, null)
    }

    override fun getChildren(): Array<StructureViewTreeElement> {
        val treeElements = mutableListOf<StructureViewTreeElement>()

        if (myElement is PsiFile) {
            myElement.acceptChildren(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val type = element.node.elementType

                    // Add explicitly parsed statements
                    if (type == RenPyScriptElementTypes.LABEL_STMT || type == RenPyScriptElementTypes.MENU_STMT) {
                        treeElements.add(RenPyStructureViewElement(element))
                    }
                    // Add our generic keyword declarations
                    else if (type == RenPyScriptElementTypes.GEN_STMT_KEYWORD) {
                        val keywordToken = element.firstChild?.node?.elementType
                        val targetTokens = setOf(
                            RenPyScriptTokenTypes.SCREEN_KEYWORD,
                            RenPyScriptTokenTypes.TRANSFORM_KEYWORD,
                            RenPyScriptTokenTypes.DEFINE_KEYWORD,
                            RenPyScriptTokenTypes.DEFAULT_KEYWORD,
                            RenPyScriptTokenTypes.INIT_KEYWORD,
                            RenPyScriptTokenTypes.PYTHON_KEYWORD,
                            RenPyScriptTokenTypes.STYLE_KEYWORD
                        )
                        if (targetTokens.contains(keywordToken)) {
                            treeElements.add(RenPyStructureViewElement(element))
                        }
                    }

                    super.visitElement(element)
                }
            })
        }

        return treeElements.toTypedArray()
    }
}