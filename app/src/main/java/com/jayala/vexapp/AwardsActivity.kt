package com.jayala.vexapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jayala.vexapp.databinding.ActivityAwardsBinding
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AwardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAwardsBinding

    companion object {
        private const val START_DATE = "2023-08-01T00:00:00Z"
        private val SEASON_IDS = (181..210).toList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAwardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        val teamId = sharedPref.getInt("team_id", -1)
        val teamName = sharedPref.getString("team_full_name", "Team Info")
        binding.teamName.text = teamName

        setupUI()

        if (teamId != -1) {
            fetchAwardsData(teamId)
        } else {
            showError("Invalid Team ID")
        }
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener { finish() }
        binding.awardsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Ensure initial state is clean
        binding.progressBar.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
    }

    private fun fetchAwardsData(teamId: Int) {
        lifecycleScope.launch {
            setLoading(true)

            try {
                val eventsResponse = RetrofitClient.service.getTeamEvents(teamId, startDate = START_DATE)
                val awardsResponse = RetrofitClient.service.getTeamAwards(teamId, SEASON_IDS)

                if (eventsResponse.isSuccessful && awardsResponse.isSuccessful) {
                    val eventsData = eventsResponse.body()?.data ?: emptyList()
                    val awardsData = awardsResponse.body()?.data ?: emptyList()

                    val uiModels = processAwards(eventsData, awardsData)
                    updateUI(uiModels)
                } else {
                    showError("Server error: ${awardsResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e("VEX_DEBUG", "Fetch Failure", e)
                showError("Check your connection and try again.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun processAwards(events: List<EventDetail>, awards: List<AwardData>): List<AwardUiModel> {
        val eventDateMap = events.associate { it.id to it.start }

        return awards.map { award ->
            val rawDate = eventDateMap[award.event?.id] ?: ""

            val cleanedTitle = award.title.replace(Regex("\\(.*?\\)"), "").trim()

            AwardUiModel(
                title = cleanedTitle,
                eventName = award.event?.name ?: "Unknown Event",
                displayDate = formatIsoDate(rawDate),
                sortableDate = rawDate
            )
        }.sortedByDescending { it.sortableDate }
    }

    private fun formatIsoDate(isoString: String): String {
        return try {
            if (isoString.isEmpty()) return "TBD"
            val parsed = ZonedDateTime.parse(isoString)
            parsed.format(DateTimeFormatter.ofPattern("MM/dd/yy", Locale.US))
        } catch (_: Exception) {
            "TBD"
        }
    }

    private fun updateUI(models: List<AwardUiModel>) {
        val awardCount = models.size
        val subtitleText = getString(R.string.trophies_and_achievements_count, awardCount)

        val spannableSubtitle = android.text.SpannableString(subtitleText)

        val startIndex = subtitleText.indexOf("(")

        if (startIndex != -1) {
            spannableSubtitle.setSpan(
                android.text.style.ForegroundColorSpan(
                    androidx.core.content.ContextCompat.getColor(this, R.color.accent_gold)
                ),
                startIndex,
                subtitleText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (models.isEmpty()) {
            binding.awardsRecyclerView.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
            binding.awardsSubtitle.text = spannableSubtitle
        } else {
            binding.awardsRecyclerView.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            binding.awardsRecyclerView.adapter = AwardsAdapter(models)
            binding.awardsSubtitle.text = spannableSubtitle
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.errorLayout.visibility = View.GONE
            binding.awardsRecyclerView.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.errorText.text = message
        binding.awardsRecyclerView.visibility = View.GONE
        // Add a retry button in your XML that calls fetchAwardsData(teamId)
        binding.retryButton.setOnClickListener {
            val teamId = intent.getIntExtra("TEAM_ID", -1)
            fetchAwardsData(teamId)
        }
    }
}