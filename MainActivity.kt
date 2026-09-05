package com.depthwallpaper.app

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.depthwallpaper.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { loadAndProcess(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pickButton.setOnClickListener { pickImage.launch("image/*") }

        binding.setWallpaperButton.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, DepthWallpaperService::class.java)
            )
            startActivity(intent)
        }
    }

    private fun loadAndProcess(uri: Uri) {
        binding.statusText.setText(R.string.processing)
        binding.setWallpaperButton.isEnabled = false

        val source: Bitmap = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return

        binding.previewImage.setImageBitmap(source)

        lifecycleScope.launch {
            try {
                val foreground = SegmentationHelper.extractForeground(source)
                // The background is stored as the plain photo; DepthWallpaperService
                // applies the real-time blur via RenderEffect at draw time.
                val background = source.copy(Bitmap.Config.ARGB_8888, false)

                WallpaperStore.save(this@MainActivity, foreground, background)

                binding.statusText.setText(R.string.ready)
                binding.setWallpaperButton.isEnabled = true
            } catch (e: Exception) {
                binding.statusText.text = getString(R.string.processing) + " failed: ${e.message}"
            }
        }
    }
}
