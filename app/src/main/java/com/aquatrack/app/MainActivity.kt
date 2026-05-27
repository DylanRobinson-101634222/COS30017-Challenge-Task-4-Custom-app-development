package com.aquatrack.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var topAppBar: MaterialToolbar
    private var defaultToolbarColor: Int = 0
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_main)
        topAppBar = findViewById(R.id.topAppBar)
        defaultToolbarColor = ContextCompat.getColor(this, R.color.aquatrack_surface)
        setSupportActionBar(topAppBar)
        ensureNotificationPermissionIfNeeded()

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        appBarConfiguration = AppBarConfiguration(navHost.navController.graph)
        setupActionBarWithNavController(navHost.navController, appBarConfiguration)

        navHost.navController.addOnDestinationChangedListener { _, destination, _ ->
            applyToolbarStyleForDestination(destination.id)
        }
    }

    private fun applyToolbarStyleForDestination(destinationId: Int) {
        val isHome = destinationId == R.id.tankListFragment
        topAppBar.title = if (isHome) "" else getString(R.string.app_name)
        topAppBar.setBackgroundColor(if (isHome) android.graphics.Color.TRANSPARENT else defaultToolbarColor)
        topAppBar.elevation = 0f
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return NavigationUI.navigateUp(navHost.navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
