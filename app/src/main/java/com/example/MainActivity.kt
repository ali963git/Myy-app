package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.ThakiroonRepository
import com.example.ui.ThakiroonMainScreen
import com.example.ui.theme.ThakiroonTheme
import com.example.viewmodel.ThakiroonViewModel
import com.example.viewmodel.ThakiroonViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set up local SQLite Room database, DAOs, and repository layer
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ThakiroonRepository(database.bookmarkDao())

        // Build main ViewModel with full state machines via Factory pattern
        val viewModel: ThakiroonViewModel by viewModels {
            ThakiroonViewModelFactory(repository)
        }

        setContent {
            ThakiroonTheme {
                ThakiroonMainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
