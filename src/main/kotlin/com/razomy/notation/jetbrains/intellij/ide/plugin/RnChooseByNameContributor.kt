package com.razomy.notation.jetbrains.intellij.ide.plugin
import com.intellij.util.Processor;
import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter

import java.util.Objects;

internal class RnChooseByNameContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        val project: Project = Objects.requireNonNull<Project>(scope.project)
        val propertyKeys: List<String> = ContainerUtil.map(
            RnUtil.findProperties(project), RnProperty::getKey
        )
        ContainerUtil.process(propertyKeys, processor)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        val properties = RnUtil.findProperties(parameters.project, name)
            .map { it as NavigationItem }
        ContainerUtil.process(properties, processor)
    }
}