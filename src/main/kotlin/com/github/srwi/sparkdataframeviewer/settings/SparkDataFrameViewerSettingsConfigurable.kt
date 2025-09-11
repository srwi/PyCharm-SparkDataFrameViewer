package com.github.srwi.sparkdataframeviewer.settings

import com.intellij.openapi.options.Configurable
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.JComboBox

class SparkDataFrameViewerSettingsConfigurable : Configurable {
    private var applyLimitCheckBox: JCheckBox? = null
    private var queryLimitSpinner: JSpinner? = null
    private var viewTargetCombo: JComboBox<SparkDataFrameViewerSettingsState.ViewTarget>? = null

    override fun createComponent(): JComponent? {
        applyLimitCheckBox = JCheckBox("Apply limit")
        queryLimitSpinner = JSpinner(SpinnerNumberModel(1, 1, Int.MAX_VALUE, 1))
        viewTargetCombo = JComboBox(SparkDataFrameViewerSettingsState.ViewTarget.values())

        applyLimitCheckBox!!.addActionListener {
            queryLimitSpinner!!.isEnabled = applyLimitCheckBox!!.isSelected
        }

        return FormBuilder.createFormBuilder()
            .addComponent(applyLimitCheckBox!!)
            .addLabeledComponent("Query limit:", queryLimitSpinner!!)
            .addLabeledComponent("View mode:", viewTargetCombo!!)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = SparkDataFrameViewerSettingsState.instance
        val currentLimit = (queryLimitSpinner!!.value as Number).toInt()
        val selectedTarget = viewTargetCombo!!.selectedItem as SparkDataFrameViewerSettingsState.ViewTarget
        return settings.applyLimit != applyLimitCheckBox!!.isSelected ||
                settings.queryLimit != currentLimit ||
                settings.viewTarget != selectedTarget
    }

    override fun apply() {
        val settings = SparkDataFrameViewerSettingsState.instance
        settings.applyLimit = applyLimitCheckBox!!.isSelected
        settings.queryLimit = (queryLimitSpinner!!.value as Number).toInt().coerceAtLeast(1)
        settings.viewTarget = viewTargetCombo!!.selectedItem as SparkDataFrameViewerSettingsState.ViewTarget
    }

    override fun reset() {
        val settings = SparkDataFrameViewerSettingsState.instance
        applyLimitCheckBox!!.isSelected = settings.applyLimit
        queryLimitSpinner!!.value = settings.queryLimit.coerceAtLeast(1)
        queryLimitSpinner!!.isEnabled = applyLimitCheckBox!!.isSelected
        viewTargetCombo!!.selectedItem = settings.viewTarget
    }

    override fun getDisplayName(): String {
        return "Spark DataFrame Viewer"
    }
}