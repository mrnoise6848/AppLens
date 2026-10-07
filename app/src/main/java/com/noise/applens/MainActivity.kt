package com.noise.applens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.noise.applens.state.AppLensViewModel
import com.noise.applens.ui.theme.AppLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Kept outside the composition so the inventory and scan state survive rotation.
        val viewModel = ViewModelProvider(this)[AppLensViewModel::class.java]

        setContent {
            AppLensTheme {
                AppLensApp(viewModel = viewModel)
            }
        }
    }
}
