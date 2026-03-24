package com.example.communitys.view.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.communitys.databinding.FragmentRewardsBinding
import com.example.communitys.model.data.RewardItemModel
import com.example.communitys.viewmodel.RewardsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.util.Locale

class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RewardsViewModel

    private lateinit var adapter: RewardAdapter
    private var allItems: List<RewardItemModel> = emptyList()
    private var selectedCategory: String = "all"  // "all" | "food" | "school_supplies" | "hygiene" | "household"
    private var currentPoints = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(RewardsViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCategoryTabs()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserPoints()
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = RewardAdapter(
            items         = emptyList(),
            currentPoints = currentPoints,
            onClaim       = { item -> showConfirmDialog(item) }
        )
        binding.rvRewards.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRewards.adapter       = adapter
        binding.rvRewards.isNestedScrollingEnabled = false
    }

    // ── Category tabs ─────────────────────────────────────────────────────────

    private fun setupCategoryTabs() {
        val tabs = listOf(
            binding.btnTabAll      to "all",
            binding.btnTabFood     to "food",
            binding.btnTabSchool   to "school_supplies",
            binding.btnTabHygiene  to "hygiene",
            binding.btnTabHousehold to "household"
        )
        tabs.forEach { (btn, category) ->
            btn.setOnClickListener {
                selectedCategory = category
                applyFilter()
                updateTabHighlight(tabs, btn)
            }
        }
        // Start with "All" highlighted
        updateTabHighlight(tabs, binding.btnTabAll)
    }

    private fun updateTabHighlight(
        tabs: List<Pair<MaterialButton, String>>,
        selected: MaterialButton
    ) {
        tabs.forEach { (btn, _) ->
            btn.strokeWidth = if (btn == selected) 3 else 0
            btn.strokeColor = requireContext().getColorStateList(android.R.color.white)
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun setupObservers() {
        viewModel.totalPoints.observe(viewLifecycleOwner) { pts ->
            currentPoints = pts
            updatePointsDisplay()
            adapter.updatePoints(pts)
        }

        viewModel.rewardItems.observe(viewLifecycleOwner) { items ->
            allItems = items
            applyFilter()
            updateProgressLabel()
        }

        viewModel.claimState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RewardsViewModel.ClaimState.Success -> showSuccessDialog(state.itemName)
                is RewardsViewModel.ClaimState.Error   -> showErrorDialog(state.message)
                is RewardsViewModel.ClaimState.Loading -> { /* handled by disabled button */ }
            }
        }
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    private fun applyFilter() {
        val filtered = if (selectedCategory == "all") allItems
                       else allItems.filter { it.category == selectedCategory }
        adapter.submitList(filtered)
        binding.tvEmptyRewards.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // ── Points display ────────────────────────────────────────────────────────

    private fun updatePointsDisplay() {
        val fmt = NumberFormat.getNumberInstance(Locale.US)
        binding.tvTotalPoints.text = fmt.format(currentPoints)
        updateProgressLabel()
    }

    private fun updateProgressLabel() {
        val fmt = NumberFormat.getNumberInstance(Locale.US)
        val sorted = allItems.sortedBy { it.pointsRequired }
        val next = sorted.firstOrNull { it.pointsRequired > currentPoints }
        if (next != null) {
            val prev = sorted.getOrNull(sorted.indexOf(next) - 1)?.pointsRequired ?: 0
            val range = next.pointsRequired - prev
            val progress = if (range > 0)
                (((currentPoints - prev).toFloat() / range) * 100).toInt().coerceIn(0, 100)
            else 100
            binding.progressPoints.progress = progress
            binding.tvProgressLabel.text =
                "${fmt.format(next.pointsRequired - currentPoints)} pts to \"${next.name}\""
        } else if (allItems.isNotEmpty()) {
            binding.progressPoints.progress = 100
            binding.tvProgressLabel.text = "🎉 You can claim all available rewards!"
        } else {
            binding.progressPoints.progress = 0
            binding.tvProgressLabel.text = "Loading rewards..."
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private fun showConfirmDialog(item: RewardItemModel) {
        val fmt = NumberFormat.getNumberInstance(Locale.US)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Redeem \"${item.name}\"?")
            .setMessage(
                "This costs ${fmt.format(item.pointsRequired)} points.\n" +
                "You'll have ${fmt.format(currentPoints - item.pointsRequired)} points remaining.\n\n" +
                "You must pick up the reward in person at the barangay hall."
            )
            .setPositiveButton("Yes, Request!") { _, _ ->
                viewModel.claimReward(item.id, item.name, item.pointsRequired)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSuccessDialog(itemName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("✅ Redemption Requested!")
            .setMessage(
                "Your request for \"$itemName\" has been submitted.\n\n" +
                "Please visit the barangay hall in person with a valid ID to claim your reward. " +
                "An official will verify and process your request within 3–5 business days."
            )
            .setPositiveButton("Got it!", null)
            .show()
        viewModel.loadRewardItems()  // refresh stock
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Could Not Redeem")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
