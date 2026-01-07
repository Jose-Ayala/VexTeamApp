package com.jayala.vexapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivityCompsBinding
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

class CompsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompsBinding
    private var eventList: List<CompEventDetail> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val teamId = intent.getIntExtra("TEAM_ID", -1)

        binding.backButton.setOnClickListener { finish() }

        if (teamId != -1) {
            loadDropdownData(teamId)
        }
    }

    private fun loadDropdownData(teamId: Int) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val response = RetrofitClient.service.getCompEvents(teamId)

                if (response.isSuccessful && response.body() != null) {
                    eventList = response.body()!!.data.reversed()
                    val names = mutableListOf(getString(R.string.select_event_hint))
                    names.addAll(eventList.map { it.name })

                    val adapter = ArrayAdapter(this@CompsActivity, R.layout.spinner_item, names)
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    binding.competitionDropdown.adapter = adapter

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
                    fetchEventDetails(teamId, eventList[pos - 1].id)
                } else {
                    binding.detailsContainer.removeAllViews()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchEventDetails(teamId: Int, eventId: Int) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.contentScroll.visibility = View.GONE

                val seasons = (180..215).toList() // Future-proofed range
                val sRes = RetrofitClient.service.getCompSkills(teamId, seasons)
                val rRes = RetrofitClient.service.getCompRankings(teamId, seasons)
                val aRes = RetrofitClient.service.getCompAwards(teamId, seasons)

                if (sRes.isSuccessful) {
                    val skills = sRes.body()?.data?.filter { it.event?.id == eventId } ?: emptyList()
                    val rank = rRes.body()?.data?.find { it.event?.id == eventId }
                    val awards = aRes.body()?.data?.filter { it.event?.id == eventId } ?: emptyList()

                    updateUI(skills, rank, awards)
                }
            } catch (e: Exception) {
                Log.e("COMP_DEBUG", "Fetch error", e)
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.contentScroll.visibility = View.VISIBLE
            }
        }
    }

    private fun updateUI(skills: List<CompSkillData>, rank: CompRankingData?, awards: List<CompAwardData>) {
        binding.detailsContainer.removeAllViews()

        // Helper to add sections using professional styling
        fun addSection(title: String, body: String) {
            val header = TextView(this).apply {
                text = title
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 32, 0, 8)
            }
            val content = TextView(this).apply {
                text = body
                textSize = 15f
                setTextColor("#ADB5BD".toColorInt())
                setPadding(0, 0, 0, 16)
            }
            binding.detailsContainer.addView(header)
            binding.detailsContainer.addView(content)
        }

        // Processing scores
        val driver = skills.filter { it.type.equals("driver", true) }.maxOfOrNull { it.score } ?: 0
        val prog = skills.filter { it.type.equals("programming", true) }.maxOfOrNull { it.score } ?: 0

        addSection(getString(R.string.skills), "Driver: $driver\nProgramming: $prog\nTotal: ${driver + prog}")

        val rankText = rank?.let { "Rank: ${it.rank}\nRecord: ${it.wins}W - ${it.losses}L - ${it.ties}T" }
            ?: "No ranking data available."
        addSection(getString(R.string.competitions), rankText)

        val awardText = if (awards.isEmpty()) "No awards won at this event." else awards.joinToString("\n") { "• ${it.title}" }
        addSection(getString(R.string.awards), awardText)
    }
}