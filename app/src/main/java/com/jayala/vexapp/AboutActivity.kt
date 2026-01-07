package com.jayala.vexapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.jayala.vexapp.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure this line uses ActivityAboutBinding
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // This reference should now turn purple/resolve
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.privacyPolicyButton.setOnClickListener {
            val url = "https://github.com/Jose-Ayala/VexTeamAPP/blob/main/PRIVACY_POLICY.md"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }
    }
}