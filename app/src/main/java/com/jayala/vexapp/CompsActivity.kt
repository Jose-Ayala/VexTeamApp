package com.jayala.vexapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivityCompsBinding
import kotlinx.coroutines.launch
import android.widget.LinearLayout
import android.widget.ImageView
import android.view.Gravity
import com.google.android.material.card.MaterialCardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt

class CompsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompsBinding
    private var eventList: List<CompEventDetail> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        val teamId = sharedPref.getInt("team_id", -1)
        val teamName = sharedPref.getString("team_full_name", "Team Info")
        binding.teamName.text = teamName

        binding.backButton.setOnClickListener { finish() }

        if (teamId != -1) {
            loadDropdownData(teamId)
        }
    }

    private fun formatDate(rawDate: String?): String {
        if (rawDate.isNullOrEmpty() || rawDate.length < 10) return "Date TBD"
        return try {
            val year = rawDate.substring(0, 4)
            val month = rawDate.substring(5, 7)
            val day = rawDate.substring(8, 10)
            "$month-$day-$year"
        } catch (_: Exception) {
            "Date TBD"
        }
    }

    private fun loadDropdownData(teamId: Int) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val seasonsResp = RetrofitClient.service.getSeasons(active = true)
                val activeSeasonIds = seasonsResp.body()?.data?.map { it.id } ?: emptyList()

                val response = RetrofitClient.service.getCompEvents(teamId)

                if (response.isSuccessful && response.body() != null) {
                    eventList = response.body()!!.data.filter { event ->
                        activeSeasonIds.contains(event.season?.id)
                    }.reversed()

                    val today = java.time.LocalDate.now()
                    var preselectedIndex = 0
                    val formattedNames = mutableListOf<CharSequence>(getString(R.string.select_event_hint))

                    eventList.forEachIndexed { index, event ->
                        val dateStr = formatDate(event.start)
                        val fullText = "${event.name} - $dateStr"

                        val spannable = android.text.SpannableString(fullText)
                        val dateStartIndex = fullText.lastIndexOf(dateStr)
                        if (dateStartIndex != -1) {
                            spannable.setSpan(
                                ForegroundColorSpan(ContextCompat.getColor(this@CompsActivity, R.color.accent_gold)),
                                dateStartIndex,
                                fullText.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        formattedNames.add(spannable)

                        try {
                            val eventDate = java.time.LocalDate.parse(event.start?.substring(0, 10))
                            if (preselectedIndex == 0 && !eventDate.isAfter(today)) {
                                preselectedIndex = index + 1
                            }
                        } catch (e: Exception) {
                            Log.e("COMP_DEBUG", "Date parse error", e)
                        }
                    }

                    val adapter = ArrayAdapter<CharSequence>(this@CompsActivity, R.layout.spinner_item, formattedNames)
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    binding.competitionDropdown.adapter = adapter

                    if (preselectedIndex > 0) {
                        binding.competitionDropdown.setSelection(preselectedIndex)
                        val selectedEvent = eventList[preselectedIndex - 1]
                        fetchEventDetails(teamId, selectedEvent.id, formatDate(selectedEvent.start))
                    }

                    setupDropdownListener(teamId)
                }
            } catch (e: Exception) {
                Log.e("COMP_DEBUG", "Dropdown error", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupDropdownListener(teamId: Int) {
        binding.competitionDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos > 0) {
                    val selectedEvent = eventList[pos - 1]
                    fetchEventDetails(teamId, selectedEvent.id, formatDate(selectedEvent.start))
                } else {
                    binding.detailsContainer.removeAllViews()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchEventDetails(teamId: Int, eventId: Int, formattedDate: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.contentScroll.visibility = View.GONE

                val seasons = (180..215).toList()
                val sRes = RetrofitClient.service.getCompSkills(teamId, seasons)
                val rRes = RetrofitClient.service.getCompRankings(teamId, seasons)
                val aRes = RetrofitClient.service.getCompAwards(teamId, seasons)

                if (sRes.isSuccessful) {
                    val eventSkills = sRes.body()?.data?.filter { it.event?.id == eventId } ?: emptyList()
                    val rank = rRes.body()?.data?.find { it.event?.id == eventId }
                    val awards = aRes.body()?.data?.filter { it.event?.id == eventId } ?: emptyList()

                    // Extract the rank from the first available skill entry for this event
                    val finalSkillsRank = eventSkills.firstOrNull()?.rank ?: 0

                    updateUI(eventSkills, rank, awards, formattedDate, finalSkillsRank)
                }
            } catch (e: Exception) {
                Log.e("COMP_DEBUG", "Fetch error", e)
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.contentScroll.visibility = View.VISIBLE
            }
        }
    }

    private fun updateUI(skills: List<CompSkillData>, rank: CompRankingData?, awards: List<CompAwardData>, eventDate: String, skillsRank: Int) {
        binding.detailsContainer.removeAllViews()

        fun addSection(title: String, body: String, iconRes: Int, secondaryText: String? = null, isSkills: Boolean = false) {
            val card = MaterialCardView(this).apply {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 40)
                layoutParams = params

                radius = 45f
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_dark))
                strokeColor = ContextCompat.getColor(context, R.color.divider_dark)
                strokeWidth = 3
                cardElevation = 0f
            }

            val horizontalLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(50, 50, 50, 50)
                gravity = Gravity.TOP
            }

            val icon = ImageView(this).apply {
                val iconParams = LinearLayout.LayoutParams(100, 100)
                iconParams.setMargins(0, 0, 40, 0)
                layoutParams = iconParams
                setImageResource(iconRes)
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val header = TextView(this).apply {
                text = title
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
            }

            textLayout.addView(header)

            if (isSkills) {
                val parts = body.split("\n")

                // Driver & Programming line (Blue)
                val topInfo = TextView(this).apply {
                    text = parts[0]
                    textSize = 14f
                    setTextColor("#00D2FF".toColorInt())
                    setPadding(0, 10, 0, 0)
                }
                // Total line (Accent Gold)
                val totalInfo = TextView(this).apply {
                    text = parts.getOrNull(1) ?: ""
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.accent_gold))
                    setPadding(0, 5, 0, 0)
                }
                textLayout.addView(topInfo)
                textLayout.addView(totalInfo)

                // Rank line (Green)
                parts.getOrNull(2)?.let { rankLine ->
                    val rankInfo = TextView(this).apply {
                        text = rankLine
                        textSize = 14f
                        setTextColor("#4CAF50".toColorInt())
                        setPadding(0, 5, 0, 0)
                    }
                    textLayout.addView(rankInfo)
                }
            } else {
                val content = TextView(this).apply {
                    textSize = 14f
                    setPadding(0, 10, 0, 0)

                    if (title == getString(R.string.competitions) && body.contains("\n")) {
                        // Apply multiple colors for the Competitions card
                        val parts = body.split("\n")
                        val rankLine = parts[0]
                        val recordLine = parts[1]

                        val builder = SpannableStringBuilder()
                        builder.append(rankLine)
                        builder.setSpan(ForegroundColorSpan("#4CAF50".toColorInt()), 0, rankLine.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        builder.append("\n")
                        val recordStart = builder.length
                        builder.append(recordLine)
                        builder.setSpan(ForegroundColorSpan("#00D2FF".toColorInt()), recordStart, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setText(builder, TextView.BufferType.SPANNABLE)
                    } else {
                        // Apply single color for other cards (like Awards)
                        text = body
                        // Set the color for Awards card text back to cyber blue
                        setTextColor("#00D2FF".toColorInt())
                    }
                }
                textLayout.addView(content)
            }

            if (secondaryText != null) {
                val dateView = TextView(this).apply {
                    text = secondaryText
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.accent_gold))
                    setPadding(0, 20, 0, 0)
                }
                textLayout.addView(dateView)
            }

            horizontalLayout.addView(icon)
            horizontalLayout.addView(textLayout)
            card.addView(horizontalLayout)
            binding.detailsContainer.addView(card)
        }

        val d = skills.filter { it.type.equals("driver", true) }.maxOfOrNull { it.score } ?: 0
        val p = skills.filter { it.type.equals("programming", true) }.maxOfOrNull { it.score } ?: 0
        val total = d + p

        val skillsBody = StringBuilder("Driver: $d | Prog: $p\nTotal: $total")
        if (skillsRank > 0) {
            skillsBody.append("\nRank: $skillsRank")
        }

        addSection(getString(R.string.skills), skillsBody.toString(), R.drawable.ic_skills, isSkills = true)

        val rankText = rank?.let { "Rank: ${it.rank}\nRecord: ${it.wins}W - ${it.losses}L - ${it.ties}T" } ?: "No ranking data available."
        addSection(getString(R.string.competitions), rankText, R.drawable.ic_calendar, secondaryText = "Date: $eventDate")

        val awardText = if (awards.isEmpty()) "No awards won." else awards.joinToString("\n") { "• ${it.title}" }
        addSection(getString(R.string.awards), awardText, R.drawable.ic_trophy)
    }
}
