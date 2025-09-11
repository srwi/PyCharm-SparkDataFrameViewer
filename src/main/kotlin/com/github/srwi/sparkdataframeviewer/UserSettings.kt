package com.github.srwi.sparkdataframeviewer

import com.intellij.ide.util.PropertiesComponent
import java.time.LocalDateTime

class UserSettings {
    companion object {
        private fun getPropertiesComponent(): PropertiesComponent {
            return PropertiesComponent.getInstance()
        }

        var firstUseDate: LocalDateTime?
            get() = getPropertiesComponent().getValue("firstUseDate")?.let { LocalDateTime.parse(it) }
            set(value) = getPropertiesComponent().setValue("firstUseDate", value?.toString())

        var supportReminderShown: Boolean
            get() = getPropertiesComponent().getBoolean("supportReminderShown", false)
            set(value) = getPropertiesComponent().setValue("supportReminderShown", value)
    }
}