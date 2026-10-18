package org.razomy.notation

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import kotlin.math.min

class RnRunConfigurationProducer : LazyRunConfigurationProducer<RnConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return RnConfigurationType.getInstance().configurationFactories[0]
    }

    override fun setupConfigurationFromContext(
        configuration: RnConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        var contextLocation = context.location
        if (contextLocation == null) {
            return false
        }
        if (contextLocation.getPsiElement() is PsiCompiledElement) {
            return false
        }
        var element = contextLocation.getPsiElement()
        if (!element.isPhysical) {
            return false
        }

        if (element.text.length < 4) {
            return false
        }

        val file = context.psiLocation?.containingFile ?: return false
        if (!file.name.endsWith(".rn")) return false

        configuration.name = "Run ${file.name}"
        configuration.scriptPath = file.virtualFile.path

        val document = PsiDocumentManager.getInstance(context.project)
            .getDocument(file)

        if (document != null) {
            var command = element.text.replace("^`".toRegex(), "")
                .replace("`$".toRegex(), "")
                .replace("^run ".toRegex(), "")
            configuration.scriptCommand = command
            configuration.name = "Run ${file.name} ${command.substring(0, min(command.length, 10))}"
        }

        return true
    }

    override fun isConfigurationFromContext(
        configuration: RnConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val file = context.psiLocation?.containingFile ?: return false
        return configuration.scriptPath == file.virtualFile.path
    }
}