package com.jayala.vexapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivitySkillsBinding
import kotlinx.coroutines.launch

class SkillsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        val teamId = sharedPref.getInt("team_id", -1)
        val teamName = sharedPref.getString("team_full_name", "Team Info")
        binding.teamName.text = teamName

        if (teamId != -1) {
            fetchSkillsData(teamId)
        } else {
            showError(getString(R.string.err_network))
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.retryButton.setOnClickListener {
            if (teamId != -1) fetchSkillsData(teamId)
        }
    }

    private fun fetchSkillsData(teamId: Int) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                Log.d("VEX_DEBUG", "Fetching skills for Team ID: $teamId")

                val allSeasonIds = (180..215).toList()
                val response = RetrofitClient.service.getTeamSkills(
                    teamId = teamId,
                    seasons = allSeasonIds
                )

                if (response.isSuccessful && response.body() != null) {
                    val rawData = response.body()!!.data

                    val groupedByEvent = rawData.groupBy { dataItem: SkillsData ->
                        dataItem.event.name to dataItem.season.name
                    }

                    val uiModels = groupedByEvent.map { (key, entries) ->
                        val (eventName, seasonName) = key

                        val driver = entries.find { it.type.equals("driver", true) }?.score ?: 0
                        val programming = entries.find { it.type.equals("programming", true) }?.score ?: 0
                        val rank = entries.firstOrNull()?.rank ?: 0

                        SkillsUiModel(
                            eventName = eventName,
                            seasonName = seasonName,
                            driverScore = driver,
                            programmingScore = programming,
                            totalScore = driver + programming,
                            rank = rank
                        )
                    }.reversed()

                    updateUI(uiModels)

                    Log.d("VEX_DEBUG", "Successfully displayed ${uiModels.size} merged events")

                } else {
                    Log.e("VEX_DEBUG", "API Error: ${response.code()}")
                    showError(getString(R.string.err_network))
                }
            } catch (e: Exception) {
                Log.e("VEX_DEBUG", "Skills Fetch Failed", e)
                showError(getString(R.string.check_internet))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updateUI(models: List<SkillsUiModel>) {
        if (models.isEmpty()) {
            binding.skillsRecyclerView.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
        } else {
            binding.skillsRecyclerView.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            binding.skillsRecyclerView.adapter = SkillsAdapter(models)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.errorLayout.visibility = View.GONE
            binding.skillsRecyclerView.visibility = View.GONE
            binding.emptyStateText.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.errorText.text = message
        binding.skillsRecyclerView.visibility = View.GONE
        binding.emptyStateText.visibility = View.GONE
    }
}

data class SkillsUiModel(
    val eventName: String,
    val seasonName: String,
    val driverScore: Int,
    val programmingScore: Int,
    val totalScore: Int,
    val rank: Int
)