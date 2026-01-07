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

        val teamId = intent.getIntExtra("TEAM_ID", -1)

        if (teamId != -1) {
            fetchSkillsData(teamId)
        } else {
            // Using the string resource we added to the strings.xml
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

                // Updated range to include the current 2025-2026 and future seasons
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

                        // Use ignoreCase for professional string comparison
                        val driver = entries.find { it.type.equals("driver", true) }?.score ?: 0
                        val programming = entries.find { it.type.equals("programming", true) }?.score ?: 0

                        SkillsUiModel(
                            eventName = eventName,
                            seasonName = seasonName,
                            driverScore = driver,
                            programmingScore = programming,
                            totalScore = driver + programming
                        )
                    }.reversed() // Kept your chronological order

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
    val totalScore: Int
)