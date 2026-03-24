package com.example.communitys.view.rewards

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.communitys.databinding.ItemRewardBinding
import com.example.communitys.model.data.RewardItemModel
import java.text.NumberFormat
import java.util.Locale

class RewardAdapter(
    private var items: List<RewardItemModel>,
    private var currentPoints: Int,
    private val onClaim: (RewardItemModel) -> Unit
) : RecyclerView.Adapter<RewardAdapter.ViewHolder>() {

    // Category → stripe color
    private val categoryColors = mapOf(
        "food"            to "#FF7043",
        "school_supplies" to "#1565C0",
        "hygiene"         to "#2E7D32",
        "household"       to "#6A1B9A"
    )

    inner class ViewHolder(val binding: ItemRewardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRewardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val fmt  = NumberFormat.getNumberInstance(Locale.US)
        with(holder.binding) {
            tvRewardName.text        = item.name
            tvRewardDescription.text = item.description ?: ""
            tvRewardPoints.text      = "${fmt.format(item.pointsRequired)} pts"

            // Category color stripe
            val color = categoryColors[item.category] ?: "#FFA726"
            viewCategoryStripe.setBackgroundColor(Color.parseColor(color))

            val canAfford   = currentPoints >= item.pointsRequired
            val outOfStock  = item.stock <= 0

            when {
                outOfStock -> {
                    btnClaimReward.isEnabled = false
                    btnClaimReward.text      = "Out of Stock"
                    btnClaimReward.backgroundTintList =
                        holder.itemView.context.getColorStateList(android.R.color.darker_gray)
                    root.alpha = 0.6f
                }
                !canAfford -> {
                    val needed = item.pointsRequired - currentPoints
                    btnClaimReward.isEnabled = false
                    btnClaimReward.text      = "Need ${fmt.format(needed)} more"
                    btnClaimReward.backgroundTintList =
                        holder.itemView.context.getColorStateList(android.R.color.darker_gray)
                    root.alpha = 0.7f
                }
                else -> {
                    btnClaimReward.isEnabled = true
                    btnClaimReward.text      = "Redeem"
                    btnClaimReward.backgroundTintList =
                        holder.itemView.context.getColorStateList(android.R.color.holo_orange_light)
                    root.alpha = 1f
                }
            }

            btnClaimReward.setOnClickListener {
                if (canAfford && !outOfStock) onClaim(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updatePoints(newPoints: Int) {
        currentPoints = newPoints
        notifyDataSetChanged()
    }

    fun submitList(newItems: List<RewardItemModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}
