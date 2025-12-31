package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ScriptRunnerUtil
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.notification.Notification

class OpenInAppAction : AnAction() {

    fun getCommand(): String? {
        var id = this.templatePresentation.text
        return when (id) {
            "WebStorm" -> "webstorm"
            "PyCharm" -> "pycharm"
            "Idea" -> "idea"
            else -> {
                throw UnknownError("Unknown command: $id")
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        // 1. Get selected file
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val filePath = virtualFile.path

        try {
            // 2. Build command: sysprocess add <file>
            val commandLine = GeneralCommandLine(getCommand(), filePath)

            // 3. Run execution
            val output = ScriptRunnerUtil.getProcessOutput(commandLine)

            // 4. Notify success
            notify("Success: $output", NotificationType.INFORMATION)

        } catch (ex: Exception) {
            notify("Error: ${ex.message}", NotificationType.ERROR)
        }
    }

    private fun notify(content: String, type: NotificationType) {
        Notifications.Bus.notify(
            Notification("Run Anything", "OpenInApp", content, type)
        )
    }
}