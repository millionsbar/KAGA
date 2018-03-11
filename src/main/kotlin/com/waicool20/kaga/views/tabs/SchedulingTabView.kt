/*
 * GPLv3 License
 *
 *  Copyright (c) KAGA by waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.waicool20.kaga.views.tabs

import com.waicool20.kaga.Kaga
import javafx.beans.property.DoubleProperty
import javafx.beans.property.StringProperty
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import org.controlsfx.control.RangeSlider
import tornadofx.*
import java.time.LocalTime
import kotlin.math.roundToInt


class SchedulingTabView {
    @FXML private lateinit var enableSleepButton: CheckBox
    @FXML private lateinit var sleepRangeSlider: RangeSlider
    @FXML private lateinit var sleepTimeLabel: Label

    @FXML private lateinit var enableExpSleepButton: CheckBox
    @FXML private lateinit var expSleepRangeSlider: RangeSlider
    @FXML private lateinit var expSleepTimeLabel: Label

    @FXML private lateinit var enableSortieSleepButton: CheckBox
    @FXML private lateinit var sortieSleepRangeSlider: RangeSlider
    @FXML private lateinit var sortieSleepTimeLabel: Label

    @FXML private lateinit var sleepContent: VBox
    @FXML private lateinit var expSleepContent: VBox
    @FXML private lateinit var sortieSleepContent: VBox

    @FXML
    fun initialize() {
        setValues()
        createBindings()
    }

    private class SleepContainer(
            val slider: RangeSlider,
            val label: Label,
            val startTime: StringProperty,
            val length: DoubleProperty
    ) {
        fun setup() {
            val sTime = String.format("%04d", startTime.get().toInt()).let {
                LocalTime.of(it.substring(0, 2).toInt(), it.substring(2, 4).toInt())
            }
            val endTime = sTime.plusMinutes((length.get() * 60).toLong())
            slider.lowValue = sTime.hour + (sTime.minute / 60.0)
            slider.highValue = endTime.hour + (endTime.minute / 60.0)
            updateTime()
            slider.lowValueProperty().addListener { _ -> updateTime() }
            slider.highValueProperty().addListener { _ -> updateTime() }
        }

        fun updateTime() {
            with(slider) {
                val startHour = lowValue.toInt()
                val startMinute = ((lowValue - startHour) * 60).toInt()
                val sTime = formatTime(startHour, startMinute)

                val endHour = highValue.toInt()
                val endMinute = ((highValue - endHour) * 60).toInt()
                val endTime = formatTime(endHour, endMinute)

                label.text = "$sTime - $endTime"

                startTime.set(sTime.replace(":", ""))
                length.set(((highValue - lowValue) * 100).roundToInt() / 100.0)
            }
        }

        private fun formatTime(hour: Int, minute: Int) = String.format("%02d:%02d", hour, minute)
    }

    private fun setValues() {
        with(Kaga.PROFILE.scheduledSleep) {
            listOf(
                    SleepContainer(sleepRangeSlider, sleepTimeLabel, sleepStartTimeProperty, sleepLengthProperty),
                    SleepContainer(expSleepRangeSlider, expSleepTimeLabel, expSleepStartTimeProperty, expSleepLengthProperty),
                    SleepContainer(sortieSleepRangeSlider, sortieSleepTimeLabel, sortieSleepStartTimeProperty, sortieSleepLengthProperty)
            ).forEach { it.setup() }
        }
    }

    private fun createBindings() {
        with (Kaga.PROFILE.scheduledSleep) {
            enableSleepButton.bind(sleepEnabledProperty)
            enableExpSleepButton.bind(expSleepEnabledProperty)
            enableSortieSleepButton.bind(sortieSleepEnabledProperty)
        }
        sleepContent.disableProperty().bind(enableSleepButton.selectedProperty().not())
        expSleepContent.disableProperty().bind(enableExpSleepButton.selectedProperty().not())
        sortieSleepContent.disableProperty().bind(enableSortieSleepButton.selectedProperty().not())
    }
}
