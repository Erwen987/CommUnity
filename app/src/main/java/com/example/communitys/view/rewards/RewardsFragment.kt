package com.example.communitys.view.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.communitys.databinding.FragmentRewardsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.util.Locale

class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!

    // TODO: Replace with value loaded from Supabase
    private var currentPoints = 1234

    data class Reward(val name: String, val emoji: String, val cost: Int)

    private val rewards = listOf(
        Reward("Prepaid Load",    "📱", 100),
        Reward("School Supplies", "🎒", 300),
        Reward("₱500 Groceries",  "🛒", 500),
        Reward("₱3,000 Cash",     "💰", 1500)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePointsDisplay()
        updateCardStates()
        setupClickListeners()
    }

    // ─── Points Display + Progress Bar ────────────────────────────────────────

    private fun updatePointsDisplay() {
        val fmt = NumberFormat.getNumberInstance(Locale.US)
        binding.tvTotalPoints.text = fmt.format(currentPoints)

        val nextReward = rewards.firstOrNull { it.cost > currentPoints }
        if (nextReward != null) {
            val prevCost = rewards.getOrNull(rewards.indexOf(nextReward) - 1)?.cost ?: 0
            val range = nextReward.cost - prevCost
            val progress = (((currentPoints - prevCost).toFloat() / range) * 100).toInt().coerceIn(0, 100)
            binding.progressPoints.progress = progress
            binding.tvProgressLabel.text =
                "${fmt.format(nextReward.cost - currentPoints)} pts to ${nextReward.name} ${nextReward.emoji}"
        } else {
            binding.progressPoints.progress = 100
            binding.tvProgressLabel.text = "🎉 You can claim all rewards!"
        }
    }

    // ─── Lock/Unlock Cards ────────────────────────────────────────────────────

    private fun updateCardStates() {
        val fmt = NumberFormat.getNumberInstance(Locale.US)
        val items = listOf(
            Triple(binding.cvRewardPrepaidLoad,    binding.btnClaimPrepaidLoad,    rewards[0].cost),
            Triple(binding.cvRewardSchoolSupplies, binding.btnClaimSchoolSupplies, rewards[1].cost),
            Triple(binding.cvRewardGroceries500,   binding.btnClaimGroceries500,   rewards[2].cost),
            Triple(binding.cvRewardGroceries3000,  binding.btnClaimCash3000,       rewards[3].cost)
        )
        items.forEach { (card, btn, cost) ->
            val canAfford = currentPoints >= cost
            card.alpha = if (canAfford) 1f else 0.5f
            btn.isEnabled = canAfford
            btn.text = if (canAfford) "Claim Reward"
            else "🔒 Need ${fmt.format(cost - currentPoints)} pts"
            btn.backgroundTintList = requireContext().getColorStateList(
                if (canAfford) android.R.color.holo_orange_light
                else android.R.color.darker_gray
            )
        }
    }

    // ─── Button Listeners ─────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnClaimPrepaidLoad.setOnClickListener    { showConfirmDialog(rewards[0]) }
        binding.btnClaimSchoolSupplies.setOnClickListener { showConfirmDialog(rewards[1]) }
        binding.btnClaimGroceries500.setOnClickListener   { showConfirmDialog(rewards[2]) }
        binding.btnClaimCash3000.setOnClickListener       { showConfirmDialog(rewards[3]) }
    }

    // ─── Confirm → Claim → Success ────────────────────────────────────────────

    private fun showConfirmDialog(reward: Reward) {
        val fmt = NumberFormat.getNumberInstance(Locale.US)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Claim ${reward.name}? ${reward.emoji}")
            .setMessage(
                "This costs ${fmt.format(reward.cost)} points.\n" +
                        "You'll have ${fmt.format(currentPoints - reward.cost)} points remaining."
            )
            .setPositiveButton("Yes, Claim!") { _, _ -> processClaim(reward) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processClaim(reward: Reward) {
        if (currentPoints < reward.cost) return
        currentPoints -= reward.cost

        // TODO: Update points in Supabase & save claim record
        // viewModel.claimReward(reward.name, reward.cost)

        updatePointsDisplay()
        updateCardStates()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🎉 Reward Claimed!")
            .setMessage(
                "You've claimed ${reward.name} ${reward.emoji}!\n\n" +
                        "A barangay official will contact you within 3–5 business days."
            )
            .setPositiveButton("Got it!", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}