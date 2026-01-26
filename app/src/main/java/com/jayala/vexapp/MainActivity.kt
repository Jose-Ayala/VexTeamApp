package com.jayala.vexapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)

        if (sharedPref.getInt("team_id", -1) != -1) {
            navigateToHome()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchButton.setOnClickListener {
            val teamInput = binding.teamNumberSearch.text.toString().trim().uppercase()

            if (validateFormat(teamInput)) {
                verifyTeamExists(teamInput)
            }
        }
    }

    private fun validateFormat(input: String): Boolean {
        val vexRegex = Regex("^[0-9A-Z]{2,10}$")
        return when {
            input.isEmpty() -> {
                binding.teamNumberInputLayout.error = "Please enter a team number"
                false
            }
            !input.matches(vexRegex) -> {
                binding.teamNumberInputLayout.error = "Use format like 1234A"
                false
            }
            else -> {
                binding.teamNumberInputLayout.error = null
                true
            }
        }
    }

    private fun verifyTeamExists(teamNumber: String) {
        lifecycleScope.launch {
            try {
                binding.teamNumberInputLayout.error = null
                binding.teamRecyclerView.visibility = View.GONE
                binding.resultsLabel.visibility = View.GONE

                binding.searchButton.isEnabled = false
                binding.searchButton.text = getString(R.string.verifying)

                val response = RetrofitClient.service.getTeamInfo(teamNumber = teamNumber)

                if (response.isSuccessful && response.body() != null) {
                    val teams = response.body()!!.data

                    when {
                        teams.isEmpty() -> {
                            binding.teamNumberInputLayout.error = "Team not found."
                        }
                        teams.size == 1 -> {
                            saveAndNavigate(teams[0].id, teams[0].number)
                        }
                        else -> {
                            setupTeamTable(teams)
                        }
                    }
                } else {
                    binding.teamNumberInputLayout.error = "API Error. Try again."
                }
            } catch (e: Exception) {
                binding.teamNumberInputLayout.error = "Connection error."
            } finally {
                binding.searchButton.isEnabled = true
                binding.searchButton.text = "SEARCH"
            }
        }
    }

    private fun setupTeamTable(teams: List<TeamData>) {
        binding.resultsLabel.visibility = View.VISIBLE
        binding.teamRecyclerView.visibility = View.VISIBLE
        binding.teamRecyclerView.adapter = TeamAdapter(teams) { selectedTeam ->
            saveAndNavigate(selectedTeam.id, selectedTeam.number)
        }
    }

    private fun saveAndNavigate(teamId: Int, teamNumber: String) {
        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        sharedPref.edit {
            putInt("team_id", teamId)
            putString("team_number", teamNumber)
        }
        navigateToHome()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}