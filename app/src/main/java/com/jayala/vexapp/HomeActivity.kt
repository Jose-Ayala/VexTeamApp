package com.jayala.vexapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var teamId: Int? = null
    private lateinit var sharedPref: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)

        val savedNumber = sharedPref.getString("team_number", null)
        val savedId = sharedPref.getInt("team_id", -1)

        setupNavigation()

        if (savedNumber != null && savedId != -1) {
            this.teamId = savedId
            fetchTeamData(savedNumber, savedId)
        } else {
            navigateToMain()
        }

        binding.changeTeamButton.setOnClickListener {
            sharedPref.edit {
                remove("team_number")
                remove("team_id")
            }
            navigateToMain()
        }

        binding.aboutButton.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupNavigation() {
        setButtonsEnabled(false)
        binding.skillsButton.setOnClickListener { launchSection(SkillsActivity::class.java) }
        binding.competitionsButton.setOnClickListener { launchSection(CompsActivity::class.java) }
        binding.awardsButton.setOnClickListener { launchSection(AwardsActivity::class.java) }
    }

    private fun fetchTeamData(teamNumber: String, id: Int) {
        lifecycleScope.launch {
            try {
                binding.organization.text = getString(R.string.loading_team_info)

                val response = RetrofitClient.service.getTeamInfo(teamNumber = teamNumber)

                if (response.isSuccessful && response.body() != null) {
                    val team = response.body()!!.data.find { it.id == id }

                    if (team != null) {
                        val headerText = getString(R.string.team_name_format, team.number, team.team_name)
                        val spannable = android.text.SpannableString(headerText)
                        val endOfNumber = team.number.length

                        sharedPref.edit {
                            putString("team_full_name", headerText)
                        }

                        spannable.setSpan(
                            android.text.style.ForegroundColorSpan(ContextCompat.getColor(this@HomeActivity, R.color.cyber_blue)),
                            0, endOfNumber,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        spannable.setSpan(
                            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                            0, endOfNumber,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        binding.teamName.text = spannable
                        binding.programName.text = getString(R.string.program_format, team.program.code, team.program.name)
                        binding.organization.text = team.organization

                        binding.location.text = getString(
                            R.string.location_format,
                            team.location.city,
                            team.location.region,
                            team.location.country
                        )

                        setButtonsEnabled(true)
                        fetchDashboardData(team.id)
                    } else {
                        navigateToMain()
                    }
                } else {
                    navigateToMain()
                }
            } catch (e: Exception) {
                Log.e("VEX_DEBUG", "Connection failure", e)
                navigateToMain()
            }
        }
    }

    private fun fetchDashboardData(teamId: Int) {
        lifecycleScope.launch {
            try {
                val seasonsResp = RetrofitClient.service.getSeasons(active = true)
                val seasonIds = seasonsResp.body()?.data?.map { it.id } ?: emptyList()

                if (seasonIds.isNotEmpty()) {
                    val skillsResp = RetrofitClient.service.getTeamSkills(teamId, seasonIds)
                    if (skillsResp.isSuccessful) {
                        val skillsData = skillsResp.body()?.data ?: emptyList()

                        val bestRun = skillsData
                            .groupBy { it.event.id }
                            .map { entry ->
                                val driver = entry.value.filter { it.type.contains("driver", true) }.maxOfOrNull { it.score } ?: 0
                                val prog = entry.value.filter { it.type.contains("prog", true) }.maxOfOrNull { it.score } ?: 0
                                val rank = entry.value.maxByOrNull { it.score }?.rank ?: 0
                                BestSkillsRun(driver, prog, driver + prog, rank)
                            }
                            .maxByOrNull { it.total }

                        if (bestRun != null && bestRun.total > 0) {
                            updateSkillsUI(bestRun.driver, bestRun.programming, bestRun.total, bestRun.rank)
                        } else {
                            updateSkillsUI(null, null, null, null)
                        }
                    }

                    val awardsResp = RetrofitClient.service.getTeamAwards(teamId, seasonIds)
                    if (awardsResp.isSuccessful) {
                        updateAwardsUI(awardsResp.body()?.data ?: emptyList())
                    }
                }

                val eventResponse = RetrofitClient.service.getCompEvents(teamId)
                if (eventResponse.isSuccessful) {
                    val events = eventResponse.body()?.data ?: emptyList()
                    val today = LocalDate.now()
                    val formatter = DateTimeFormatter.ISO_DATE_TIME

                    val nextEvent = events
                        .filter { !it.start.isNullOrEmpty() }
                        .mapNotNull {
                            try { Pair(it, LocalDate.parse(it.start, formatter)) }
                            catch (_: Exception) { null }
                        }
                        .filter { it.second.isAfter(today.minusDays(1)) }
                        .minByOrNull { it.second }

                    if (nextEvent != null) {
                        val daysBetween = ChronoUnit.DAYS.between(today, nextEvent.second)
                        val countdownText = when (daysBetween) {
                            0L -> "Today"
                            1L -> "Tomorrow"
                            else -> "In $daysBetween days"
                        }

                        binding.nextEventName.text = nextEvent.first.name
                        val spannableCountdown = android.text.SpannableString(countdownText)
                        spannableCountdown.setSpan(
                            android.text.style.ForegroundColorSpan(ContextCompat.getColor(this@HomeActivity, R.color.accent_gold)),
                            0, countdownText.length,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.competitionsSubtitle.text = spannableCountdown
                    }
                }
            } catch (e: Exception) {
                Log.e("VEX_DEBUG", "Dashboard error: ${e.message}", e)
            }
        }
    }

    private fun updateSkillsUI(driver: Int?, prog: Int?, total: Int?, rank: Int?) {
        if (total != null && total > 0) {
            binding.skillsSeasonBest.text = getString(R.string.skills_season_best_format, total)
            binding.skillsSeasonBest.setTextColor(ContextCompat.getColor(this, R.color.accent_gold))
            binding.skillsBreakdown.visibility = View.VISIBLE
            binding.skillsBreakdown.text = getString(R.string.skills_breakdown_format, driver ?: 0, prog ?: 0)
            if (rank != null && rank > 0) {
                binding.skillsRank.visibility = View.VISIBLE
                binding.skillsRank.text = getString(R.string.rank_format, rank)
            } else {
                binding.skillsRank.visibility = View.GONE
            }
        } else {
            binding.skillsSeasonBest.text = getString(R.string.no_skills_runs)
            binding.skillsSeasonBest.setTextColor(ContextCompat.getColor(this, R.color.text_medium_emphasis))
            binding.skillsBreakdown.visibility = View.GONE
            binding.skillsRank.visibility = View.GONE
        }
    }

    private fun updateAwardsUI(awardsList: List<AwardData>) {
        binding.awardsSubtitle.text = getString(R.string.season_total_format, awardsList.size)
        if (awardsList.isNotEmpty()) {
            val awardsSummary = awardsList
                .groupBy { it.title }
                .map { entry ->
                    val cleanedTitle = entry.key.substringBefore(" (").trim()
                    getString(R.string.award_category_format, entry.value.size, cleanedTitle)
                }
                .joinToString("\n")
            binding.awardsFullList.text = awardsSummary
            binding.awardsFullList.setTextColor(ContextCompat.getColor(this, R.color.cyber_blue))
        } else {
            binding.awardsFullList.text = getString(R.string.no_awards_earned_yet)
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        val alphaValue = if (enabled) 1.0f else 0.5f
        binding.skillsButton.apply { isEnabled = enabled; alpha = alphaValue }
        binding.competitionsButton.apply { isEnabled = enabled; alpha = alphaValue }
        binding.awardsButton.apply { isEnabled = enabled; alpha = alphaValue }
    }

    private fun launchSection(destination: Class<*>) {
        teamId?.let { id ->
            val intent = Intent(this, destination)
            intent.putExtra("TEAM_ID", id)
            startActivity(intent)
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

data class BestSkillsRun(
    val driver: Int,
    val programming: Int,
    val total: Int,
    val rank: Int
)