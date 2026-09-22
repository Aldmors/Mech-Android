package com.mech.carexpensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.ui.MainViewModel
import com.mech.carexpensetracker.ui.navigation.AppNavHost
import com.mech.carexpensetracker.ui.theme.CarExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestReminderNotifications()
        enableEdgeToEdge()
        setContent {
            CarExpenseTrackerTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val ready by viewModel.isReady.collectAsStateWithLifecycle()
                val cars by viewModel.cars.collectAsStateWithLifecycle()
                val selectedCar by viewModel.selectedCar.collectAsStateWithLifecycle()
                if (!ready) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AppNavHost(
                        hasCars = cars.isNotEmpty(),
                        selectedCarId = selectedCar?.externalId,
                    )
                }
            }
        }
    }

    private fun requestReminderNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
    }
}
