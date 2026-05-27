package com.aquatrack.app.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.aquatrack.app.R
import com.aquatrack.app.data.Tank
import com.aquatrack.app.databinding.FragmentAddEditTankBinding
import com.aquatrack.app.util.ReminderFrequencyUtils
import com.aquatrack.app.util.ValidationUtils
import com.aquatrack.app.viewmodel.TankViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class AddEditTankFragment : Fragment(R.layout.fragment_add_edit_tank) {
    private var _binding: FragmentAddEditTankBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TankViewModel by viewModels()
    private var editingTank: Tank? = null
    private var pendingCameraImageUri: Uri? = null
    private var selectedImageUri: String = ""

    private val pickTankImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            applyTankImageUri(uri.toString())
        }
    }

    private val captureTankImageLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            applyTankImageUri(pendingCameraImageUri?.toString().orEmpty())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddEditTankBinding.bind(view)

        val tankId = arguments?.getLong("tankId", 0L) ?: 0L
        binding.reminderFrequencyToggleGroup.check(R.id.reminderWeeklyButton)
        binding.customReminderUnitToggleGroup.check(R.id.customReminderWeeksButton)

        // In edit mode, fill the form so the user can change only what they need.
        fun syncReminderFrequencyVisibility(enabled: Boolean) {
            binding.reminderFrequencyLabel.visibility = if (enabled) View.VISIBLE else View.GONE
            binding.reminderFrequencyToggleGroup.visibility = if (enabled) View.VISIBLE else View.GONE
            val showCustom =
                enabled && binding.reminderFrequencyToggleGroup.checkedButtonId == R.id.reminderCustomButton
            binding.customReminderIntervalRow.visibility = if (showCustom) View.VISIBLE else View.GONE
        }

        if (tankId > 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                val existing = viewModel.getTankById(tankId) ?: return@launch
                editingTank = existing
                binding.tankNameInput.setText(existing.name)
                selectedImageUri = existing.imageUri
                updateTankImagePreview(existing.imageUri)
                binding.volumeInput.setText(existing.volumeLitres.toString())
                binding.temperatureInput.setText(existing.targetTempC.toString())
                binding.reminderSwitch.isChecked = existing.reminderEnabled
                binding.tankFormTitleText.setText(R.string.edit_tank)
                when {
                    existing.waterType.contains("salt", ignoreCase = true) -> {
                        binding.waterTypeToggleGroup.check(R.id.saltwaterButton)
                    }

                    else -> {
                        binding.waterTypeToggleGroup.check(R.id.freshwaterButton)
                    }
                }

                when (existing.reminderFrequency) {
                    "Daily" -> binding.reminderFrequencyToggleGroup.check(R.id.reminderDailyButton)
                    "Custom" -> binding.reminderFrequencyToggleGroup.check(R.id.reminderCustomButton)
                    else -> binding.reminderFrequencyToggleGroup.check(R.id.reminderWeeklyButton)
                }

                ReminderFrequencyUtils.parseCustomFrequency(existing.reminderFrequency)?.let { (value, unit) ->
                    binding.reminderFrequencyToggleGroup.check(R.id.reminderCustomButton)
                    binding.customReminderValueInput.setText(value.toString())
                    binding.customReminderUnitToggleGroup.check(
                        if (unit.equals("Weeks", ignoreCase = true)) R.id.customReminderWeeksButton else R.id.customReminderDaysButton
                    )
                }

                syncReminderFrequencyVisibility(existing.reminderEnabled)
                binding.saveTankButton.setText(R.string.update_tank)
            }
        } else {
            binding.tankFormTitleText.setText(R.string.add_new_tank)
            binding.waterTypeToggleGroup.check(R.id.freshwaterButton)
            binding.reminderSwitch.isChecked = true
            syncReminderFrequencyVisibility(true)
            selectedImageUri = ""
            updateTankImagePreview(selectedImageUri)
        }

        binding.pickTankImageButton.setOnClickListener {
            pickTankImageLauncher.launch(arrayOf("image/*"))
        }

        binding.captureTankImageButton.setOnClickListener {
            val outputUri = createCameraImageUri()
            pendingCameraImageUri = outputUri
            captureTankImageLauncher.launch(outputUri)
        }

        binding.removeTankImageButton.setOnClickListener {
            selectedImageUri = ""
            updateTankImagePreview("")
        }

        binding.cancelTankButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Hide the reminder fields when the switch is off to keep the form simple.
            syncReminderFrequencyVisibility(isChecked)
        }

        binding.reminderFrequencyToggleGroup.addOnButtonCheckedListener { _, _, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            syncReminderFrequencyVisibility(binding.reminderSwitch.isChecked)
        }

        binding.saveTankButton.setOnClickListener {
            val name = ValidationUtils.parseRequiredText(binding.tankNameInput.text)
            val volume = ValidationUtils.parsePositiveInt(binding.volumeInput.text)
            val temp = ValidationUtils.parsePositiveFloat(binding.temperatureInput.text)

            var valid = true
            if (name == null) {
                binding.tankNameLayout.error = getString(R.string.error_required)
                valid = false
            } else {
                binding.tankNameLayout.error = null
            }

            if (volume == null) {
                binding.volumeLayout.error = getString(R.string.error_positive_number)
                valid = false
            } else {
                binding.volumeLayout.error = null
            }

            if (temp == null) {
                binding.temperatureLayout.error = getString(R.string.error_positive_number)
                valid = false
            } else {
                binding.temperatureLayout.error = null
            }

            if (!valid) return@setOnClickListener

            val safeName = name ?: return@setOnClickListener
            val safeVolume = volume ?: return@setOnClickListener
            val safeTemp = temp ?: return@setOnClickListener

            val selectedWaterType = when (binding.waterTypeToggleGroup.checkedButtonId) {
                R.id.saltwaterButton -> getString(R.string.water_type_salt)
                else -> getString(R.string.water_type_fresh)
            }

            val selectedReminderFrequency = when (binding.reminderFrequencyToggleGroup.checkedButtonId) {
                R.id.reminderDailyButton -> "Daily"
                R.id.reminderCustomButton -> {
                    val customValue = ValidationUtils.parsePositiveInt(binding.customReminderValueInput.text)

                    if (binding.reminderSwitch.isChecked && customValue == null) {
                        binding.customReminderValueLayout.error = getString(R.string.error_positive_number)
                        return@setOnClickListener
                    }

                    binding.customReminderValueLayout.error = null
                    // Store custom reminders in one string so the detail screen and scheduler can read it later.
                    val unit = if (binding.customReminderUnitToggleGroup.checkedButtonId == R.id.customReminderDaysButton) {
                        "Days"
                    } else {
                        "Weeks"
                    }
                    ReminderFrequencyUtils.buildCustomFrequency(
                        value = customValue ?: ReminderFrequencyUtils.DEFAULT_CUSTOM_VALUE,
                        unit = unit
                    )
                }

                else -> "Weekly"
            }

            val imageUri = selectedImageUri.trim()

            val existing = editingTank
            val tank = Tank(
                id = existing?.id ?: 0,
                name = safeName,
                volumeLitres = safeVolume,
                waterType = selectedWaterType,
                targetTempC = safeTemp,
                lastCleanedEpochMillis = existing?.lastCleanedEpochMillis ?: System.currentTimeMillis(),
                reminderEnabled = binding.reminderSwitch.isChecked,
                reminderFrequency = selectedReminderFrequency,
                reminderTime = existing?.reminderTime ?: "19:00",
                imageUri = imageUri
            )
            viewModel.saveTank(tank)
            Toast.makeText(requireContext(), getString(R.string.tank_saved_message), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyTankImageUri(value: String) {
        selectedImageUri = value
        updateTankImagePreview(selectedImageUri)
    }

    private fun updateTankImagePreview(uriValue: String) {
        if (uriValue.isBlank()) {
            binding.tankImagePreview.setImageDrawable(null)
        } else {
            binding.tankImagePreview.load(uriValue)
        }
    }

    private fun createCameraImageUri(): Uri {
        val cameraDir = File(requireContext().cacheDir, "camera").apply { mkdirs() }
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFile = File(cameraDir, "tank_${time}.jpg")
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
    }
}
