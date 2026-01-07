package com.jayala.vexapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var teamId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        val savedTeam = sharedPref.getString("team_number", null)

        setupNavigation()

        if (savedTeam != null) {
            fetchTeamData(savedTeam)
        } else {
            navigateToMain()
        }

        binding.changeTeamButton.setOnClickListener {
            sharedPref.edit { remove("team_number") }
            navigateToMain()
        }

        binding.aboutButton.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupNavigation() {
        // Initially disable buttons until teamId is loaded
        setButtonsEnabled(false)

        binding.skillsButton.setOnClickListener {
            launchSection(SkillsActivity::class.java)
        }

        binding.competitionsButton.setOnClickListener {
            launchSection(CompsActivity::class.java)
        }

        binding.awardsButton.setOnClickListener {
            launchSection(AwardsActivity::class.java)
        }
    }

    private fun fetchTeamData(teamNumber: String) {
        lifecycleScope.launch {
            try {
                // Show loading status in the UI
                binding.organization.text = getString(R.string.loading_team_info)

                val response = RetrofitClient.service.getTeamInfo(teamNumber = teamNumber)

                if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                    val team = response.body()!!.data[0]
                    teamId = team.id

                    // Populate UI
                    binding.teamName.text = getString(R.string.team_name_format, team.number, team.team_name)
                    binding.programName.text = getString(R.string.program_format, team.program.code, team.program.name)
                    binding.organization.text = team.organization
                    binding.location.text = getString(R.string.location_format, team.location.city, team.location.region)
                    binding.country.text = team.location.country

                    // Data is ready, enable the buttons
                    setButtonsEnabled(true)
                } else {
                    Log.e("VEX_DEBUG", "API Error: ${response.code()}")
                    Toast.makeText(this@HomeActivity, "Team not found", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
            } catch (e: Exception) {
                Log.e("VEX_DEBUG", "Connection failure", e)
                binding.organization.text = getString(R.string.connection_error)
                Toast.makeText(this@HomeActivity, "Please check your internet connection.", Toast.LENGTH_LONG).show()
            }
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