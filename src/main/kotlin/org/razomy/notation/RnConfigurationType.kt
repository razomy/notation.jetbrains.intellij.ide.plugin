package org.razomy.notation

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.nio.charset.StandardCharsets
import javax.swing.JComponent


class RnConfigurationOptions : RunConfigurationOptions() {
    var scriptPath: String? by string()
    var scriptCommand: String? by string()
}

class RnConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<RnConfigurationOptions>(project, factory, name) {

    var scriptPath: String?
        get() = state?.scriptPath
        set(value) {
            state?.scriptPath = value
        }

    var scriptCommand: String?
        get() = state?.scriptCommand
        set(value) {
            state?.scriptCommand = value
        }

    override fun getOptionsClass() = RnConfigurationOptions::class.java

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return RnSettingsEditor()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return RnProfileState(environment, this)
    }

    override fun checkConfiguration() {
        if (scriptPath.isNullOrEmpty()) throw RuntimeConfigurationException("Script path is missing")
    }
}


class RnProfileState(
    environment: ExecutionEnvironment,
    private val config: RnConfiguration
) : CommandLineState(environment) {

    override fun startProcess(): ProcessHandler {
        var args = config.scriptCommand?.split(" ") ?: List(1, { " " })
        var command = args[0]
        var arguments = args.subList(1, args.size)
        val commandLine = GeneralCommandLine()
            .withExePath(command)
            .withParameters(arguments)
            .withCharset(StandardCharsets.UTF_8)
            .withWorkDirectory(environment.project.basePath)

        val processHandler = ProcessHandlerFactory.getInstance()
            .createColoredProcessHandler(commandLine)

        ProcessTerminatedListener.attach(processHandler)
        return processHandler
    }
}


class RnSettingsEditor : SettingsEditor<RnConfiguration>() {
    private lateinit var panel: DialogPanel

    private var scriptPathState = ""
    private var scriptCommandState = ""

    override fun createEditor(): JComponent {
        panel = panel {
            row("Script File:") {
                textFieldWithBrowseButton()
                    .bindText(::scriptPathState)
            }
            row("Script Command:") {
                textField()
                    .bindText(::scriptCommandState)
            }
        }
        return panel
    }

    override fun resetEditorFrom(config: RnConfiguration) {
        scriptPathState = config.scriptPath ?: ""
        scriptCommandState = config.scriptCommand ?: ""
        panel.reset()
    }

    override fun applyEditorTo(config: RnConfiguration) {
        panel.apply()
        config.scriptPath = scriptPathState
        config.scriptCommand = scriptCommandState
    }
}


class RnConfigurationType : ConfigurationTypeBase(
    "RnConfiguration",
    "Rn",
    "Run rn scripts",
    RnIcons.FILE
) {
    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun getId() = "RnConfiguration"
            override fun createTemplateConfiguration(project: Project) =
                RnConfiguration(project, this, "My Run Config")

            override fun getOptionsClass() = RnConfigurationOptions::class.java
        })
    }

    companion object {
        fun getInstance(): RnConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(RnConfigurationType::class.java)
    }
}