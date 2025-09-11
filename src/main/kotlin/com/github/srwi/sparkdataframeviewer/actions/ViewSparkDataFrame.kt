package com.github.srwi.sparkdataframeviewer.actions

import com.github.srwi.sparkdataframeviewer.interop.Python
import com.github.srwi.sparkdataframeviewer.settings.SparkDataFrameViewerSettingsState
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyFrameAccessor
import com.jetbrains.python.debugger.containerview.PyDataView
import javax.swing.SwingUtilities
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.jetbrains.python.debugger.containerview.PyDataViewDialog

class ViewSparkDataFrame : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return

        // It may be possible to view as pandas dataframe without the debugger, but for now we warn the user instead
        if (catchJupyterVarFrame(value.frameAccessor, project)) {
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading data...", true) {
            override fun run(progressIndicator: ProgressIndicator) {
                try {
                    val variableName = getExpression(value)
                    val expression = buildExpression(variableName)
                    val pandasValue = Python.evaluateExpression(value.frameAccessor, expression)
                    SwingUtilities.invokeLater {
                        openDataViewer(project, pandasValue)
                    }
                } catch (e: InterruptedException) {
                    // Operation cancelled by user
                } catch (e: OutOfMemoryError) {
                    Notifications.Bus.notify(
                        Notification(
                            "notificationGroup.error",
                            "Out of memory",
                            "The IDE ran out of memory while trying to view the dataframe. Please try again with a smaller slice of the data.",
                            NotificationType.ERROR
                        ),
                        project
                    )
                } catch (e: Throwable) {
                    val formattedException = e.toString() + "\n" + e.stackTrace.joinToString("\n")
                    Notifications.Bus.notify(
                        Notification(
                            "notificationGroup.error",
                            "Unexpected error",
                            "Spark DataFrame Viewer encountered an unexpected error. If possible, please report this issue on GitHub.",
                            NotificationType.ERROR
                        ).addAction(CopyAndReportExceptionAction("Copy exception", formattedException))
                            .addAction(ReportToGithubAction("Report on GitHub", formattedException)),
                        project
                    )
                }
            }
        })
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        try {
            val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue
            e.presentation.isVisible = value.type == "DataFrame" && value.typeQualifier.toString().contains("pyspark")
            e.presentation.isEnabled = true
        } catch (_: Exception) {
            e.presentation.isEnabledAndVisible = false
        }
    }

    fun buildExpression(variableName: String): String {
        val settings = SparkDataFrameViewerSettingsState.instance
        val expression = if (settings.applyLimit) {
            "$variableName.limit(${settings.queryLimit}).toPandas()"
        } else {
            "$variableName.toPandas()"
        }
        return expression
    }

    fun openDataViewer(project: Project, pandasValue: PyDebugValue) {
        val settings = SparkDataFrameViewerSettingsState.instance

        when (settings.viewTarget) {
            SparkDataFrameViewerSettingsState.ViewTarget.DEFAULT -> {
                PyDataView.getInstance(project).show(pandasValue)
            }

            SparkDataFrameViewerSettingsState.ViewTarget.TOOL_WINDOW -> {
                val toolWindowManager = ToolWindowManager.getInstance(project)
                var toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_NAME)
                if (toolWindow == null) {
                    toolWindow = toolWindowManager.registerToolWindow(
                        RegisterToolWindowTask(id = TOOL_WINDOW_NAME, canCloseContent = true)
                    )
                    PyDataView.getInstance(project).init(toolWindow)
                }
                PyDataView.getInstance(project).show(pandasValue)
            }

            SparkDataFrameViewerSettingsState.ViewTarget.DIALOG -> {
                PyDataViewDialog(project, pandasValue).show()
            }
        }
    }

    private fun getExpression(value: PyDebugValue): String {
        // Usually we would use 'evaluationExpression' to get the full path of the variable.
        // Inside the evaluate expression window however the result will be assigned to a temporary
        // variable and 'name' will be the full evaluation expression instead.
        return if (value.parent == null) value.name else value.evaluationExpression
    }

    private fun catchJupyterVarFrame(frameAccessor: PyFrameAccessor, project: Project): Boolean {
        val isJupyter = frameAccessor.javaClass.simpleName == "JupyterVarsFrameAccessor"

        if (isJupyter) {
            Notifications.Bus.notify(
                Notification(
                    "notificationGroup.error",
                    "Run cell in debugger to view dataframe",
                    "To view the Spark dataframe, this cell must be executed within the debugger. " +
                            "Please rerun the cell using the debugger and try viewing the dataframe again.",
                    NotificationType.WARNING
                ),
                project
            )
        }

        return isJupyter
    }

    companion object {
        const val TOOL_WINDOW_NAME = "Spark DataFrame Viewer"
    }
}