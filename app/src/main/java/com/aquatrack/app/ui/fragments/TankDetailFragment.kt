package com.aquatrack.app.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.aquatrack.app.R
import com.aquatrack.app.data.Tank
import com.aquatrack.app.databinding.FragmentTankDetailBinding
import com.aquatrack.app.ui.adapters.FishAdapter
import com.aquatrack.app.viewmodel.TankViewModel
import com.aquatrack.app.util.ReminderFrequencyUtils
import com.google.android.material.snackbar.Snackbar

class TankDetailFragment : Fragment(R.layout.fragment_tank_detail) {
    private var _binding: FragmentTankDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TankViewModel by viewModels()
    private var currentTank: Tank? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTankDetailBinding.bind(view)

        val tankId = requireArguments().getLong("tankId", 0L)

        val fishAdapter = FishAdapter(
            onClick = { /* row tap reserved for future quick actions */ },
            onEdit = { fish ->
                findNavController().navigate(
                    R.id.action_tankDetailFragment_to_addEditFishFragment,
                    Bundle().apply {
                        putLong("tankId", tankId)
                        putLong("fishId", fish.id)
                    }
                )
            },
            onDelete = { fish ->
                viewModel.deleteFish(fish)
                Snackbar.make(binding.root, getString(R.string.fish_deleted_message), Snackbar.LENGTH_SHORT).show()
            }
        )
        binding.fishRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.fishRecyclerView.adapter = fishAdapter

        viewModel.tankById(tankId).observe(viewLifecycleOwner) { tank ->
            currentTank = tank
            if (tank == null) {
                binding.tankNameText.text = getString(R.string.tank_name_placeholder)
                binding.tankSubMetaText.text = getString(R.string.tank_sub_meta_placeholder)
                binding.cleanedStatText.text = getString(R.string.stat_today)
                binding.nextCleanStatText.text = getString(R.string.stat_next_clean_default)
                binding.reminderButton.text = getString(R.string.reminder_summary_default)
                fishAdapter.setTankTargetTemp(null)
                binding.tankHeaderImageView.visibility = View.GONE
                binding.tankHeaderOverlay.visibility = View.GONE
                binding.tankHeaderImageView.setImageDrawable(null)
                return@observe
            }

            fishAdapter.setTankTargetTemp(tank.targetTempC)

            if (tank.imageUri.isBlank()) {
                binding.tankHeaderImageView.visibility = View.GONE
                binding.tankHeaderOverlay.visibility = View.GONE
                binding.tankHeaderImageView.setImageDrawable(null)
            } else {
                binding.tankHeaderImageView.visibility = View.VISIBLE
                binding.tankHeaderOverlay.visibility = View.VISIBLE
                binding.tankHeaderImageView.load(tank.imageUri) {
                    crossfade(true)
                }
            }

            binding.tankNameText.text = tank.name
            binding.tankSubMetaText.text = getString(
                R.string.tank_sub_meta_template,
                tank.waterType,
                tank.volumeLitres,
                tank.targetTempC
            )
            binding.cleanedStatText.text = getCleanedStatLabel(tank.lastCleanedEpochMillis)
            binding.nextCleanStatText.text = getNextCleanLabel(tank.reminderFrequency)
            binding.reminderButton.text = getString(
                R.string.reminder_summary_template,
                formatReminderFrequencyLabel(tank.reminderFrequency),
                tank.reminderTime,
                if (tank.reminderEnabled) getString(R.string.reminder_on) else getString(R.string.reminder_off)
            )
        }

        viewModel.fishForTank(tankId).observe(viewLifecycleOwner) { fish ->
            fishAdapter.submitList(fish)
            binding.fishCountStatText.text = fish.size.toString()
            val hasItems = fish.isNotEmpty()
            binding.fishRecyclerView.visibility = if (hasItems) View.VISIBLE else View.GONE
            binding.emptyFishStateText.visibility = if (hasItems) View.GONE else View.VISIBLE
        }

        binding.editTankButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_tankDetailFragment_to_addEditTankFragment,
                Bundle().apply { putLong("tankId", tankId) }
            )
        }

        binding.addFishButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_tankDetailFragment_to_addEditFishFragment,
                Bundle().apply { putLong("tankId", tankId) }
            )
        }

        binding.markCleanedButton.setOnClickListener {
            val tank = currentTank ?: return@setOnClickListener
            val previousCleanedTime = tank.lastCleanedEpochMillis
            val updated = tank.copy(lastCleanedEpochMillis = System.currentTimeMillis())
            viewModel.saveTank(updated)
            Snackbar.make(binding.root, getString(R.string.cleaned_snackbar), Snackbar.LENGTH_LONG)
                .setAction(R.string.undo_action) {
                    viewModel.saveTank(tank.copy(lastCleanedEpochMillis = previousCleanedTime))
                }
                .show()
        }

        binding.reminderButton.setOnClickListener {
            val tank = currentTank ?: return@setOnClickListener
            ReminderDialogFragment
                .newInstance(
                    frequency = tank.reminderFrequency,
                    time = tank.reminderTime,
                    enabled = tank.reminderEnabled
                )
                .show(parentFragmentManager, "reminder_dialog")
        }

        setFragmentResultListener(ReminderDialogFragment.RESULT_KEY) { _, bundle ->
            val tank = currentTank ?: return@setFragmentResultListener
            val frequency = bundle.getString(ReminderDialogFragment.BUNDLE_FREQUENCY, tank.reminderFrequency)
            val time = bundle.getString(ReminderDialogFragment.BUNDLE_TIME, tank.reminderTime)
            val enabled = bundle.getBoolean(ReminderDialogFragment.BUNDLE_ENABLED, tank.reminderEnabled)

            viewModel.saveTank(
                tank.copy(
                    reminderFrequency = frequency,
                    reminderTime = time,
                    reminderEnabled = enabled
                )
            )

            Snackbar.make(binding.root, getString(R.string.reminder_saved_message), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun getCleanedStatLabel(lastCleanedMillis: Long): String {
        val days = ((System.currentTimeMillis() - lastCleanedMillis) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0)
        return if (days == 0L) getString(R.string.stat_today) else getString(R.string.stat_days, days)
    }

    private fun getNextCleanLabel(frequency: String): String {
        ReminderFrequencyUtils.parseCustomFrequency(frequency)?.let { (value, unit) ->
            return if (unit.equals("Days", ignoreCase = true)) {
                getString(R.string.stat_next_custom_days, value)
            } else {
                getString(R.string.stat_next_custom_weeks, value)
            }
        }

        return when (frequency) {
            "Daily" -> getString(R.string.stat_next_1_day)
            "Custom" -> getString(R.string.stat_next_custom)
            else -> getString(R.string.stat_next_7_days)
        }
    }

    private fun formatReminderFrequencyLabel(frequency: String): String {
        ReminderFrequencyUtils.parseCustomFrequency(frequency)?.let { (value, unit) ->
            return if (unit.equals("Days", ignoreCase = true)) {
                getString(R.string.reminder_custom_every_days, value)
            } else {
                getString(R.string.reminder_custom_every_weeks, value)
            }
        }

        return frequency
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
