package com.aquatrack.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.aquatrack.app.R
import com.aquatrack.app.data.Tank
import java.util.concurrent.TimeUnit

class TankAdapter(
    private val onClick: (Tank) -> Unit,
    private val onEdit: (Tank) -> Unit,
    private val onDelete: (Tank) -> Unit
) : ListAdapter<Tank, TankAdapter.TankViewHolder>(Diff) {
    private var fishCountsByTankId: Map<Long, Int> = emptyMap()

    fun setFishCounts(countsByTankId: Map<Long, Int>) {
        fishCountsByTankId = countsByTankId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TankViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tank, parent, false)
        return TankViewHolder(view)
    }

    override fun onBindViewHolder(holder: TankViewHolder, position: Int) {
        val tank = getItem(position)
        holder.bind(tank, fishCountsByTankId[tank.id] ?: 0)
        holder.itemView.setOnClickListener { onClick(tank) }
        holder.itemView.setOnLongClickListener {
            showContextMenu(holder.itemView, tank)
            true
        }
    }

    private fun showContextMenu(anchor: View, tank: Tank) {
        val popup = PopupMenu(anchor.context, anchor)
        popup.menu.add(0, MENU_EDIT, 0, R.string.action_edit)
        popup.menu.add(0, MENU_DELETE, 1, R.string.action_delete)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_EDIT -> {
                    onEdit(tank)
                    true
                }

                MENU_DELETE -> {
                    onDelete(tank)
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    class TankViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
        private val title: TextView = root.findViewById(R.id.tankNameText)
        private val subtitle: TextView = root.findViewById(R.id.tankMetaText)
        private val temp: TextView = root.findViewById(R.id.tankTempText)
        private val cleaned: TextView = root.findViewById(R.id.tankCleanedText)
        private val fishCount: TextView = root.findViewById(R.id.tankFishCountText)
        private val tankImage: ImageView = root.findViewById(R.id.tankImageView)

        fun bind(tank: Tank, fishTotal: Int) {
            title.text = tank.name
            subtitle.text = tank.waterType
            temp.text = root.context.getString(
                R.string.tank_stats_template,
                tank.volumeLitres,
                tank.targetTempC
            )
            fishCount.text = root.context.resources.getQuantityString(
                R.plurals.tank_card_fish_count,
                fishTotal,
                fishTotal
            )

            if (tank.imageUri.isBlank()) {
                tankImage.setImageDrawable(null)
            } else {
                tankImage.load(tank.imageUri)
            }

            val elapsedMillis = System.currentTimeMillis() - tank.lastCleanedEpochMillis
            val daysAgo = TimeUnit.MILLISECONDS.toDays(elapsedMillis).coerceAtLeast(0)
            cleaned.text = root.context.resources.getQuantityString(
                R.plurals.cleaned_days_ago,
                daysAgo.toInt(),
                daysAgo
            )
        }
    }

    companion object {
        private const val MENU_EDIT = 1
        private const val MENU_DELETE = 2
    }

    private object Diff : DiffUtil.ItemCallback<Tank>() {
        override fun areItemsTheSame(oldItem: Tank, newItem: Tank): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Tank, newItem: Tank): Boolean = oldItem == newItem
    }
}
