package com.jayala.vexapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jayala.vexapp.databinding.ItemTeamRowBinding

class TeamAdapter(
    private val teams: List<TeamData>,
    private val onTeamSelected: (TeamData) -> Unit
) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>() {

    inner class TeamViewHolder(val binding: ItemTeamRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val binding = ItemTeamRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TeamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        val team = teams[position]

        holder.binding.apply {
            teamNameText.text = team.team_name
            programTypeText.text = "${team.program.code} — ${team.program.name}"
            organizationText.text = team.organization

            root.setOnClickListener {
                onTeamSelected(team)
            }
        }
    }

    override fun getItemCount(): Int = teams.size
}