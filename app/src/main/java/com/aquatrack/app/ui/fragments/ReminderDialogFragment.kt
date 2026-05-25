package com.aquatrack.app.ui.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.aquatrack.app.R
import com.aquatrack.app.databinding.DialogReminderBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class ReminderDialogFragment : DialogFragment() {
    private var _binding: DialogReminderBinding? = null
    private val binding get() = _binding!!

    private var selectedFrequency: String = "Weekly"
    private var selectedHour: Int = 19
    private var selectedMinute: Int = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogReminderBinding.inflate(layoutInflater)

        val initialFrequency = requireArguments().getString(ARG_FREQUENCY, "Weekly") ?: "Weekly"
        val initialTime = requireArguments().getString(ARG_TIME, "19:00") ?: "19:00"
        val initialEnabled = requireArguments().getBoolean(ARG_ENABLED, true)

        selectedFrequency = initialFrequency
        selectedHour = initialTime.substringBefore(':').toIntOrNull() ?: 19
        selectedMinute = initialTime.substringAfter(':').toIntOrNull() ?: 0

        binding.notificationsSwitch.isChecked = initialEnabled
        updateSelectedFrequency(initialFrequency)
        updateTimeButtonText()

        binding.frequencyToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedFrequency = when (checkedId) {
                R.id.frequencyDailyButton -> "Daily"
                R.id.frequencyCustomButton -> "Custom"
                else -> "Weekly"
            }
        }

        binding.pickTimeButton.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTitleText(R.string.reminder_time_label)
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .build()

            picker.addOnPositiveButtonClickListener {
                selectedHour = picker.hour
                selectedMinute = picker.minute
                updateTimeButtonText()
            }

            picker.show(parentFragmentManager, "time_picker")
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reminder_dialog_title)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel_action, null)
            .setPositiveButton(R.string.save_action) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY,
                    bundleOf(
                        BUNDLE_FREQUENCY to selectedFrequency,
                        BUNDLE_TIME to String.format("%02d:%02d", selectedHour, selectedMinute),
                        BUNDLE_ENABLED to binding.notificationsSwitch.isChecked
                    )
                )
            }
            .create()
    }

    private fun updateSelectedFrequency(value: String) {
        val checked = when (value) {
            "Daily" -> R.id.frequencyDailyButton
            "Custom" -> R.id.frequencyCustomButton
            else -> if (value.startsWith("Custom:", ignoreCase = true)) {
                R.id.frequencyCustomButton
            } else {
                R.id.frequencyWeeklyButton
            }
        }
        binding.frequencyToggleGroup.check(checked)
    }

    private fun updateTimeButtonText() {
        binding.pickTimeButton.text = getString(
            R.string.reminder_time_value,
            String.format("%02d:%02d", selectedHour, selectedMinute)
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_KEY = "reminder_result"
        const val BUNDLE_FREQUENCY = "frequency"
        const val BUNDLE_TIME = "time"
        const val BUNDLE_ENABLED = "enabled"

        private const val ARG_FREQUENCY = "arg_frequency"
        private const val ARG_TIME = "arg_time"
        private const val ARG_ENABLED = "arg_enabled"

        fun newInstance(frequency: String, time: String, enabled: Boolean): ReminderDialogFragment {
            return ReminderDialogFragment().apply {
                arguments = bundleOf(
                    ARG_FREQUENCY to frequency,
                    ARG_TIME to time,
                    ARG_ENABLED to enabled
                )
            }
        }
    }
}
