package com.github.srwi.sparkdataframeviewer.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "SparkDataFrameViewerSettings", storages = [Storage("SparkDataFrameViewerSettings.xml")])
class SparkDataFrameViewerSettingsState : PersistentStateComponent<SparkDataFrameViewerSettingsState> {
    var applyLimit: Boolean = true
    var queryLimit: Int = 50

    override fun getState(): SparkDataFrameViewerSettingsState {
        return this
    }

    override fun loadState(state: SparkDataFrameViewerSettingsState) {
        this.applyLimit = state.applyLimit
        this.queryLimit = state.queryLimit.coerceAtLeast(1)
    }

    companion object {
        val instance: SparkDataFrameViewerSettingsState
            get() = ApplicationManager.getApplication().getService(SparkDataFrameViewerSettingsState::class.java)
    }
}