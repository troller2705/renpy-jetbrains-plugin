package ee.vstepa.jetbrains.plugins.renpy.navigation

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.psi.PsiFile

class RenPyStructureViewModel(psiFile: PsiFile) :
    StructureViewModelBase(psiFile, RenPyStructureViewElement(psiFile)),
    StructureViewModel.ElementInfoProvider {

    override fun getSorters(): Array<Sorter> {
        // Allows the user to click the "Sort Alphabetically" button in the Structure tool window
        return arrayOf(Sorter.ALPHA_SORTER)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean {
        // Treats everything inside the file as a flat leaf node to keep the tree clean
        return element.value !is PsiFile
    }
}