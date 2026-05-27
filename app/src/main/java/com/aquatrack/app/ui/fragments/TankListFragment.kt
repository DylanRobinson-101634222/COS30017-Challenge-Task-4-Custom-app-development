package com.aquatrack.app.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aquatrack.app.R
import com.aquatrack.app.data.Fish
import com.aquatrack.app.data.Tank
import com.aquatrack.app.databinding.FragmentTankListBinding
import com.aquatrack.app.ui.adapters.TankAdapter
import com.aquatrack.app.viewmodel.TankViewModel
import com.google.android.material.snackbar.Snackbar

class TankListFragment : Fragment(R.layout.fragment_tank_list) {
    private var _binding: FragmentTankListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TankViewModel by viewModels()
    private var allTanks: List<Tank> = emptyList()
    private var allFish: List<Fish> = emptyList()
    private var currentQuery: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTankListBinding.bind(view)

        val adapter = TankAdapter(
            onClick = { tank ->
                findNavController().navigate(
                    R.id.action_tankListFragment_to_tankDetailFragment,
                    Bundle().apply { putLong("tankId", tank.id) }
                )
            },
            onEdit = { tank ->
                findNavController().navigate(
                    R.id.action_tankListFragment_to_addEditTankFragment,
                    Bundle().apply { putLong("tankId", tank.id) }
                )
            },
            onDelete = { tank ->
                viewModel.deleteTank(tank)
                Snackbar.make(binding.root, getString(R.string.tank_deleted_message), Snackbar.LENGTH_SHORT).show()
            }
        )

        binding.tankRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.tankRecyclerView.adapter = adapter

        // Keep the home stats and filtered list in sync with the same live data.
        viewModel.tanks.observe(viewLifecycleOwner) { tanks ->
            allTanks = tanks
            applySearch(adapter)
        }

        viewModel.fish.observe(viewLifecycleOwner) { fish ->
            allFish = fish
            applySearch(adapter)
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString().orEmpty()
                applySearch(adapter)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.addTankButton.setOnClickListener {
            findNavController().navigate(R.id.action_tankListFragment_to_addEditTankFragment)
        }
    }

    private fun applySearch(adapter: TankAdapter) {
        val query = currentQuery.trim()
        binding.homeTankCountStatText.text = allTanks.size.toString()
        binding.homeFishCountStatText.text = allFish.sumOf { it.quantity }.toString()
        binding.homeReminderCountStatText.text = allTanks.count { it.reminderEnabled }.toString()

        // Match tanks by tank details or by fish names, since both are useful search terms.
        val fishCountsByTankId = allFish
            .groupBy { it.tankId }
            .mapValues { (_, fishInTank) -> fishInTank.sumOf { it.quantity } }

        adapter.setFishCounts(fishCountsByTankId)

        val filtered = if (query.isBlank()) {
            allTanks
        } else {
            val matchingFishTankIds = allFish
                .asSequence()
                .filter {
                    it.speciesName.contains(query, ignoreCase = true) ||
                        it.scientificName.contains(query, ignoreCase = true)
                }
                .map { it.tankId }
                .toSet()

            allTanks.filter { tank ->
                tank.name.contains(query, ignoreCase = true) ||
                    tank.waterType.contains(query, ignoreCase = true) ||
                    tank.volumeLitres.toString().contains(query) ||
                    tank.targetTempC.toString().contains(query) ||
                    matchingFishTankIds.contains(tank.id)
            }
        }

        adapter.submitList(filtered)
        binding.tankCountText.text = getString(R.string.your_tanks_count_template, filtered.size)
        val hasItems = filtered.isNotEmpty()
        binding.tankRecyclerView.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.emptyStateText.visibility = if (hasItems) View.GONE else View.VISIBLE
        if (!hasItems) {
            binding.emptyStateText.text = if (query.isBlank()) {
                getString(R.string.no_tanks_yet)
            } else {
                getString(R.string.no_search_results)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
