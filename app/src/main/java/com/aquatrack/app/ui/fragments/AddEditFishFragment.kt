package com.aquatrack.app.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.aquatrack.app.R
import com.aquatrack.app.data.Fish
import com.aquatrack.app.databinding.FragmentAddEditFishBinding
import com.aquatrack.app.util.ValidationUtils
import com.aquatrack.app.viewmodel.TankViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class AddEditFishFragment : Fragment(R.layout.fragment_add_edit_fish) {
    private var _binding: FragmentAddEditFishBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TankViewModel by viewModels()
    private var editingFish: Fish? = null
    private var pendingCameraImageUri: Uri? = null
    private var selectedImageUri: String = ""

    private val pickFishImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            applyFishImageUri(uri.toString())
        }
    }

    private val captureFishImageLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            applyFishImageUri(pendingCameraImageUri?.toString().orEmpty())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddEditFishBinding.bind(view)

        val tankId = arguments?.getLong("tankId", 0L) ?: 0L
        val fishId = arguments?.getLong("fishId", 0L) ?: 0L

        viewLifecycleOwner.lifecycleScope.launch {
            val tank = viewModel.getTankById(tankId)
            binding.fishFormSubtitleText.text = tank?.name.orEmpty()
            binding.fishTankInput.setText(tank?.name.orEmpty())
        }

        if (fishId > 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                val existing = viewModel.getFishById(fishId) ?: return@launch
                editingFish = existing
                binding.speciesInput.setText(existing.speciesName)
                binding.scientificInput.setText(existing.scientificName)
                selectedImageUri = existing.imageUri
                updateFishImagePreview(existing.imageUri)
                binding.quantityInput.setText(existing.quantity.toString())
                binding.minTempInput.setText(existing.minTempC.toString())
                binding.maxTempInput.setText(existing.maxTempC.toString())
                val existingTank = viewModel.getTankById(existing.tankId)
                binding.fishFormSubtitleText.text = existingTank?.name.orEmpty()
                binding.fishTankInput.setText(existingTank?.name.orEmpty())
                binding.fishFormTitleText.setText(R.string.edit_fish)
                binding.saveFishButton.setText(R.string.update_fish)
            }
        } else {
            binding.fishFormTitleText.setText(R.string.add_fish)
            selectedImageUri = ""
            updateFishImagePreview(selectedImageUri)
        }

        binding.pickFishImageButton.setOnClickListener {
            pickFishImageLauncher.launch(arrayOf("image/*"))
        }

        binding.captureFishImageButton.setOnClickListener {
            val outputUri = createCameraImageUri()
            pendingCameraImageUri = outputUri
            captureFishImageLauncher.launch(outputUri)
        }

        binding.removeFishImageButton.setOnClickListener {
            selectedImageUri = ""
            updateFishImagePreview("")
        }

        binding.cancelFishButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveFishButton.setOnClickListener {
            val species = ValidationUtils.parseRequiredText(binding.speciesInput.text)
            val qty = ValidationUtils.parsePositiveInt(binding.quantityInput.text)
            val minTemp = ValidationUtils.parsePositiveFloat(binding.minTempInput.text)
            val maxTemp = ValidationUtils.parsePositiveFloat(binding.maxTempInput.text)

            var valid = true

            if (species == null) {
                binding.speciesLayout.error = getString(R.string.error_required)
                valid = false
            } else {
                binding.speciesLayout.error = null
            }

            if (qty == null) {
                binding.quantityLayout.error = getString(R.string.error_positive_number)
                valid = false
            } else {
                binding.quantityLayout.error = null
            }

            if (!ValidationUtils.isTemperatureRangeValid(minTemp, maxTemp)) {
                binding.minTempLayout.error = getString(R.string.error_temp_range)
                binding.maxTempLayout.error = getString(R.string.error_temp_range)
                valid = false
            } else {
                binding.minTempLayout.error = null
                binding.maxTempLayout.error = null
            }

            if (!valid) return@setOnClickListener

            val safeSpecies = species ?: return@setOnClickListener
            val safeQty = qty ?: return@setOnClickListener
            val safeMinTemp = minTemp ?: return@setOnClickListener
            val safeMaxTemp = maxTemp ?: return@setOnClickListener

            viewModel.saveFish(
                Fish(
                    id = editingFish?.id ?: 0,
                    tankId = editingFish?.tankId ?: tankId,
                    speciesName = safeSpecies,
                    scientificName = binding.scientificInput.text?.toString().orEmpty(),
                    quantity = safeQty,
                    minTempC = safeMinTemp,
                    maxTempC = safeMaxTemp,
                    imageUri = selectedImageUri.trim()
                )
            )
            Toast.makeText(requireContext(), getString(R.string.fish_saved_message), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyFishImageUri(value: String) {
        selectedImageUri = value
        updateFishImagePreview(selectedImageUri)
    }

    private fun updateFishImagePreview(uriValue: String) {
        if (uriValue.isBlank()) {
            binding.fishImagePreview.setImageDrawable(null)
        } else {
            binding.fishImagePreview.load(uriValue)
        }
    }

    private fun createCameraImageUri(): Uri {
        val cameraDir = File(requireContext().cacheDir, "camera").apply { mkdirs() }
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFile = File(cameraDir, "fish_${time}.jpg")
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
    }
}
