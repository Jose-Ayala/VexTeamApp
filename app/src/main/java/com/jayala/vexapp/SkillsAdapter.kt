package com.jayala.vexapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jayala.vexapp.databinding.ItemSkillRowBinding

class SkillsAdapter(private val skillsList: List<SkillsUiModel>) :
    RecyclerView.Adapter<SkillsAdapter.SkillsViewHolder>() {

    // This class finds the IDs inside item_skill_row.xml
    class SkillsViewHolder(val binding: ItemSkillRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillsViewHolder {
        val binding = ItemSkillRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SkillsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SkillsViewHolder, position: Int) {
        val skill = skillsList[position]

        holder.binding.apply {
            rowEventName.text = skill.eventName
            rowSeasonName.text = skill.seasonName
            rowScoreBreakdown.text = "Driver: ${skill.driverScore} | Prog: ${skill.programmingScore}"
            rowTotalScore.text = "${skill.totalScore}"
            rowRank.text = if (skill.rank > 0) "Rank: ${skill.rank}" else "Rank: N/A"
        }
    }

    override fun getItemCount(): Int = skillsList.size
}