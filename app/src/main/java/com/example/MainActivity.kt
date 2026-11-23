package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.KamarTajRoot
import com.example.ui.StudioViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val app = application as KamarTajApp
    setContent {
      val studioViewModel: StudioViewModel = viewModel(factory = StudioViewModel.Factory(app))
      KamarTajRoot(viewModel = studioViewModel)
    }
  }
}
