package com.github.srwi.sparkdataframeviewer.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "SparkDataFrameViewerSettings", storages = [Storage("SparkDataFrameViewerSettings.xml")])
class SparkDataFrameViewerSettingsState : PersistentStateComponent<SparkDataFrameViewerSettingsState> {
    enum class ViewTarget(val displayName: String) {
        DEFAULT("Default"),
        TOOL_WINDOW("Tool window"),
        DIALOG("Dialog");

        override fun toString(): String = displayName
    }

    var applyLimit: Boolean = true
    var queryLimit: Int = 50
    var viewTarget: ViewTarget = ViewTarget.DEFAULT

    override fun getState(): SparkDataFrameViewerSettingsState {
        return this
    }

    override fun loadState(state: SparkDataFrameViewerSettingsState) {
        this.applyLimit = state.applyLimit
        this.queryLimit = state.queryLimit.coerceAtLeast(1)
        this.viewTarget = state.viewTarget
    }

    companion object {
        val instance: SparkDataFrameViewerSettingsState
            get() = ApplicationManager.getApplication().getService(SparkDataFrameViewerSettingsState::class.java)
    }
}