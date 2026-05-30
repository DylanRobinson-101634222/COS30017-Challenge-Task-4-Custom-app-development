package com.aquatrack.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.aquatrack.app.R
import com.aquatrack.app.data.Fish
import com.google.android.material.card.MaterialCardView

class FishAdapter(
    private val onClick: (Fish) -> Unit,
    private val onEdit: (Fish) -> Unit,
    private val onDelete: (Fish) -> Unit
) : ListAdapter<Fish, FishAdapter.FishViewHolder>(Diff) {
    private var tankTargetTempC: Float? = null

    fun setTankTargetTemp(temp: Float?) {
        if (tankTargetTempC == temp) return
        tankTargetTempC = temp
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FishViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fish, parent, false)
        return FishViewHolder(view)
    }

    override fun onBindViewHolder(holder: FishViewHolder, position: Int) {
        val fish = getItem(position)
        holder.bind(fish, tankTargetTempC)
        holder.itemView.setOnClickListener { onClick(fish) }
        holder.moreActionsButton.setOnClickListener {
            showContextMenu(holder.moreActionsButton, fish)
        }
        holder.itemView.setOnLongClickListener {
            showContextMenu(holder.itemView, fish)
            true
        }
    }

    private fun showContextMenu(anchor: View, fish: Fish) {
        val popup = PopupMenu(anchor.context, anchor)
        popup.menu.add(0, MENU_EDIT, 0, R.string.action_edit)
        popup.menu.add(0, MENU_DELETE, 1, R.string.action_delete)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_EDIT -> {
                    onEdit(fish)
                    true
                }

                MENU_DELETE -> {
                    onDelete(fish)
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    class FishViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
        private val card: MaterialCardView = root as MaterialCardView
        private val title: TextView = root.findViewById(R.id.fishSpeciesText)
        private val scientific: TextView = root.findViewById(R.id.fishScientificText)
        private val subtitle: TextView = root.findViewById(R.id.fishMetaText)
        private val fishImage: ImageView = root.findViewById(R.id.fishImageView)
        val moreActionsButton: ImageView = root.findViewById(R.id.fishMoreActionsButton)

        fun bind(fish: Fish, tankTargetTempC: Float?) {
            val context = root.context
            title.text = fish.speciesName
            scientific.text = fish.scientificName.ifBlank {
                context.getString(R.string.scientific_name_not_set)
            }
            val baseMeta = context.getString(
                R.string.fish_meta_template,
                fish.quantity,
                fish.minTempC,
                fish.maxTempC
            )

            val outOfRange = tankTargetTempC?.let { it < fish.minTempC || it > fish.maxTempC } == true
            if (outOfRange) {
                subtitle.text = context.getString(R.string.fish_meta_with_temp_warning, baseMeta, tankTargetTempC)
                subtitle.setTextColor(ContextCompat.getColor(context, R.color.aquatrack_warning))
                card.strokeColor = ContextCompat.getColor(context, R.color.aquatrack_warning)
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.aquatrack_warning_container))
            } else {
                subtitle.text = baseMeta
                subtitle.setTextColor(ContextCompat.getColor(context, R.color.aquatrack_card_muted))
                card.strokeColor = ContextCompat.getColor(context, R.color.aquatrack_outline)
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.aquatrack_surface))
            }

            if (fish.imageUri.isBlank()) {
                fishImage.setImageDrawable(null)
            } else {
                fishImage.load(fish.imageUri)
            }
        }
    }

    companion object {
        private const val MENU_EDIT = 1
        private const val MENU_DELETE = 2
    }

    private object Diff : DiffUtil.ItemCallback<Fish>() {
        override fun areItemsTheSame(oldItem: Fish, newItem: Fish): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Fish, newItem: Fish): Boolean = oldItem == newItem
    }
}
