package com.mech.carexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
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
        enableEdgeToEdge()
        setContent {
            CarExpenseTrackerTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val cars by viewModel.cars.collectAsStateWithLifecycle()
                val selectedCar by viewModel.selectedCar.collectAsStateWithLifecycle()
                AppNavHost(
                    hasCars = cars.isNotEmpty(),
                    selectedCarId = selectedCar?.externalId,
                )
            }
        }
    }
}
