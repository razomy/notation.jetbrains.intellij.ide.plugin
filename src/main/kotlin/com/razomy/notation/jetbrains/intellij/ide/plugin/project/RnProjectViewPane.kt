package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.icons.AllIcons
import com.intellij.ide.SelectInTarget
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.JBUI
import com.razomy.notation.jetbrains.intellij.ide.plugin.project.ScopeLoader.loadScopes
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel


class RnProjectViewPane(project: Project) : ProjectViewPane(project) {
    private var scopes: List<ScopeDefinition?> = loadScopes(myProject)

    var ID = "RnProject"
    override fun getTitle(): String {
        return ID;
    }

    override fun getWeight(): Int {
        return 8;
    }

    override fun getId(): String {
        return ID;
    }

    private var currentScope: ScopeDefinition? = scopes.firstOrNull()
    private lateinit var scopeDropdown: ComboBox<ScopeDefinition>

    override fun createComponent(): JComponent {
        val treeComponent = super.createComponent()
        val panel = JPanel(BorderLayout())

        scopeDropdown = ComboBox<ScopeDefinition>(scopes.toTypedArray<ScopeDefinition?>())
        scopeDropdown.setSelectedItem(currentScope)
        scopeDropdown.isOpaque = false
        scopeDropdown.setBorder(JBUI.Borders.empty());
        scopeDropdown.background = JBUI.CurrentTheme.ToolWindow.background()

        scopeDropdown.addItemListener(ItemListener { e: ItemEvent? ->
            if (e!!.getStateChange() == ItemEvent.SELECTED) {
                currentScope = (e.getItem() as ScopeDefinition?)!!
                (this.treeStructure as RnProjectTreeStructure).currentScope = currentScope!!;
                updateFromRoot(true)
            }
        })

        val refreshBtn = JButton(AllIcons.Actions.Refresh)
        refreshBtn.addActionListener(ActionListener { e: ActionEvent? -> reloadScopes() })

        val topPanel = JPanel(BorderLayout())
        topPanel.add(scopeDropdown, BorderLayout.CENTER)
//TODO: remove white bg topPanel.add(refreshBtn, BorderLayout.EAST)

        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(treeComponent, BorderLayout.CENTER)
        return panel
    }

    private fun reloadScopes() {
        val scopes = loadScopes(myProject)
        scopeDropdown.setModel(DefaultComboBoxModel<ScopeDefinition?>(scopes.toTypedArray<ScopeDefinition?>()))
        scopeDropdown.setSelectedIndex(0)
    }

    override fun createComparator(): Comparator<NodeDescriptor<*>> {
        return RnOrderConfigComparator(myProject, ID)
    }

    override fun createSelectInTarget(): SelectInTarget {
        return RnProjectViewSelectInTarget(myProject, ID)
    }

    override fun createStructure(): ProjectAbstractTreeStructureBase {
        if (currentScope == null) {
            reloadScopes()
        }
        return RnProjectTreeStructure(this.myProject, ID, currentScope!!)
    }
}