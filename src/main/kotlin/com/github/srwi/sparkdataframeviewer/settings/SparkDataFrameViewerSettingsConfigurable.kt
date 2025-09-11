package com.github.srwi.sparkdataframeviewer.settings

import com.intellij.openapi.options.Configurable
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class SparkDataFrameViewerSettingsConfigurable : Configurable {
    private var applyLimitCheckBox: JCheckBox? = null
    private var queryLimitSpinner: JSpinner? = null

    override fun createComponent(): JComponent? {
        applyLimitCheckBox = JCheckBox("Apply limit")
        queryLimitSpinner = JSpinner(SpinnerNumberModel(1, 1, Int.MAX_VALUE, 1))

        applyLimitCheckBox!!.addActionListener {
            queryLimitSpinner!!.isEnabled = applyLimitCheckBox!!.isSelected
        }

        return FormBuilder.createFormBuilder()
            .addComponent(applyLimitCheckBox!!)
            .addLabeledComponent("Query limit:", queryLimitSpinner!!)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = SparkDataFrameViewerSettingsState.instance
        val currentLimit = (queryLimitSpinner!!.value as Number).toInt()
        return settings.applyLimit != applyLimitCheckBox!!.isSelected ||
                settings.queryLimit != currentLimit
    }

    override fun apply() {
        val settings = SparkDataFrameViewerSettingsState.instance
        settings.applyLimit = applyLimitCheckBox!!.isSelected
        settings.queryLimit = (queryLimitSpinner!!.value as Number).toInt().coerceAtLeast(1)
    }

    override fun reset() {
        val settings = SparkDataFrameViewerSettingsState.instance
        applyLimitCheckBox!!.isSelected = settings.applyLimit
        queryLimitSpinner!!.value = settings.queryLimit.coerceAtLeast(1)
        queryLimitSpinner!!.isEnabled = applyLimitCheckBox!!.isSelected
    }

    override fun getDisplayName(): String {
        return "Spark DataFrame Viewer"
    }
}