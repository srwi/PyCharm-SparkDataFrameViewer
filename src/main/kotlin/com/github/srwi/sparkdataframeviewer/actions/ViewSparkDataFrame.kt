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
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyFrameAccessor
import com.jetbrains.python.debugger.containerview.PyDataView
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import javax.swing.SwingUtilities
import kotlinx.coroutines.*
import java.util.concurrent.Future

class ViewSparkDataFrame : AnAction() {
    private val tempVariablesToCleanup = mutableSetOf<Triple<Project, PyFrameAccessor, String>>()

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
                    val pandasValue = executeSparkQueryWithProgress(value, progressIndicator)
                    SwingUtilities.invokeLater {
                        PyDataView.getInstance(project).show(pandasValue)

                        // Register cleanup for the temporary variable when debug stepping occurs
                        registerTempVariableCleanup(project, pandasValue.frameAccessor, pandasValue.name)
                    }
                } catch (_: InterruptedException) {
                    // Operation cancelled by user
                } catch (_: OutOfMemoryError) {
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

    private fun receiveData(clientChannel: AsynchronousSocketChannel, progressIndicator: ProgressIndicator) {
        val bufferSize = 4  // 4 bytes for a float
        val buffer = ByteBuffer.allocate(bufferSize)

        while (true) {
            buffer.clear()
            val bytesRead = clientChannel.read(buffer).get()

            if (bytesRead == -1) {
                // Connection closed by Python side
                break
            }

            if (bytesRead == bufferSize) {
                buffer.flip()
                val progressValue = buffer.float
                SwingUtilities.invokeLater {
                    progressIndicator.fraction = progressValue.toDouble()
                }
            }
        }

        if (clientChannel.isOpen) {
            clientChannel.close()
        }
    }

    fun executeSparkQueryWithProgressHandler(
        frameAccessor: PyFrameAccessor,
        expression: String,
        resultVariableName: String,
        port: Int
    ) {
        // This will execute the spark query and send progress updates via the socket connection.
        // Once the socket is closed on the Python side, the `resultVariableName` variable can be used to retrieve the result.
        val expressionWithLimit = buildExpressionWithLimit(expression)
        Python.executeStatement(frameAccessor, """
            import socket as __tmp_socket
            import struct as __tmp_struct
            
            __tmp_a_socket = None
            __tmp_progress_handler = None
            $resultVariableName = None
            
            def __tmp_get_progress_handler(socket, struct):
                def progress_handler(stages, inflight_tasks, operation_id, done):
                    if not stages:
                        socket.sendall(struct.pack('>f', 0.0))
                        return
                
                    total_tasks = sum(stage.num_tasks for stage in stages)
                    completed_tasks = sum(stage.num_completed_tasks for stage in stages)
                
                    if total_tasks > 0:
                        progress = completed_tasks / total_tasks
                    else:
                        progress = 0.0
                
                    socket.sendall(struct.pack('>f', progress))
                return progress_handler
            
            try:
                with __tmp_socket.socket(__tmp_socket.AF_INET, __tmp_socket.SOCK_STREAM) as __tmp_a_socket:
                    __tmp_a_socket.settimeout($TIMEOUT_IN_S)
                    __tmp_a_socket.connect(('localhost', $port))
                    
                    __tmp_progress_handler = __tmp_get_progress_handler(__tmp_a_socket, __tmp_struct)
                    
                    $expression.sparkSession.registerProgressHandler(__tmp_progress_handler)
                    $resultVariableName = $expressionWithLimit  # Execute Spark query
                    $expression.sparkSession.removeProgressHandler(__tmp_progress_handler)
            except ConnectionResetError:
                # Can happen if Kotlin side closes connection before Python socket has fully finished its operations.
                pass
                
            del __tmp_socket, __tmp_struct
            del __tmp_a_socket
            del __tmp_progress_handler, __tmp_get_progress_handler
        """.trimIndent())
    }

    fun buildExpressionWithLimit(variableName: String): String {
        val settings = SparkDataFrameViewerSettingsState.instance
        val expression = if (settings.applyLimit) {
            "$variableName.limit(${settings.queryLimit}).toPandas()"
        } else {
            "$variableName.toPandas()"
        }
        return expression
    }

    fun buildResultVariableName(value: PyDebugValue) : String {
        val prefix = value.evaluationExpression + "_"
        val allowedChars = ('a'..'z') + ('0'..'9')
        val randomSuffix = (1..8)
            .map { allowedChars.random() }
            .joinToString("")
        return prefix + randomSuffix
    }

    private fun executeSparkQueryWithProgress(value: PyDebugValue, progressIndicator: ProgressIndicator): PyDebugValue {
        val serverSocket = AsynchronousServerSocketChannel.open().bind(InetSocketAddress("localhost", 0))
        val port = (serverSocket.localAddress as InetSocketAddress).port
        val resultVariableName = buildResultVariableName(value)

        try {
            runBlocking {
                val pythonJob = async(Dispatchers.IO) {
                    executeSparkQueryWithProgressHandler(value.frameAccessor, value.evaluationExpression, resultVariableName, port)
                }

                // Wait for incoming connection from Python side
                val acceptFuture: Future<AsynchronousSocketChannel> = serverSocket.accept()
                val clientChannel = acceptFuture.get()
                progressIndicator.fraction = 0.01

                // Listen for progress updates until connection closes
                receiveData(clientChannel, progressIndicator)

                pythonJob.await()
            }

            return Python.evaluateExpression(value.frameAccessor, resultVariableName)

        } finally {
            try {
                serverSocket.close()
            } catch (_: Exception) {
                // Ignore close errors
            }
        }
    }

    private fun registerTempVariableCleanup(project: Project, frameAccessor: PyFrameAccessor, variableName: String) {
        tempVariablesToCleanup.add(Triple(project, frameAccessor, variableName))

        val debugSession = XDebuggerManager.getInstance(project).currentSession
        debugSession?.addSessionListener(object : XDebugSessionListener {
            override fun beforeSessionResume() {
                cleanupTempVariables()
                debugSession.removeSessionListener(this)
            }
        })
    }

    private fun cleanupTempVariables() {
        val iterator = tempVariablesToCleanup.iterator()
        while (iterator.hasNext()) {
            val (project, frameAccessor, varName) = iterator.next()
            try {
                PyDataView.getInstance(project).closeTabs { fa -> fa == frameAccessor }
                Python.executeStatement(frameAccessor, "del $varName")
            } catch (_: Exception) {
                // Variable might already be deleted
            }
            iterator.remove()
        }
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
        const val TIMEOUT_IN_S: Long = 10
    }
}