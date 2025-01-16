package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.impl.RnPropertyImpl

class RnStructureViewElement(private val myElement: NavigatablePsiElement) : StructureViewTreeElement,
    SortableTreeElement {
    override fun getValue(): Any {
        return myElement
    }

    override fun navigate(requestFocus: Boolean) {
        myElement.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return myElement.canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
        return myElement.canNavigateToSource()
    }

    override fun getAlphaSortKey(): String {
        val name = myElement.name
        return name ?: ""
    }

    override fun getPresentation(): ItemPresentation {
        val presentation: ItemPresentation? = myElement.presentation
        return if (presentation != null) presentation else PresentationData()
    }

    override fun getChildren(): Array<out TreeElement> {
        if (myElement is RnFile || myElement is RnProperty) {

            var properties = myElement.children.filterIsInstance<RnPropertyImpl>();
            val treeElements: MutableList<TreeElement> = ArrayList(properties.size)
            for (property in properties) {
                if (property.elementType == RnTypes.PROPERTY) {

                    treeElements.add(RnStructureViewElement(property))
                }
                if (property.elementType == RnTypes.KEY) {
                    treeElements.add(RnStructureViewElement(property))
                }
            }
            return treeElements.toTypedArray<TreeElement>()
        }
        return StructureViewTreeElement.EMPTY_ARRAY
    }
}

class RnStructureViewModel(editor: Editor?, psiFile: PsiFile?) :
    StructureViewModelBase(psiFile!!, editor, RnStructureViewElement(psiFile)), ElementInfoProvider {

    override fun getSorters(): Array<out Sorter> {
        return arrayOf(Sorter.ALPHA_SORTER)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean {
        return false
    }

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean {
        return element.value is RnProperty && (element.value as RnProperty).elementType != RnTypes.PROPERTY
    }

    override fun getSuitableClasses(): Array<out Class<RnProperty>> {
        return arrayOf(RnProperty::class.java)
    }
}

internal class RnStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return RnStructureViewModel(editor, psiFile)
            }
        }
    }
}