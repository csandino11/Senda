package com.senda.lecturabiblica

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.senda.lecturabiblica.ui.SendaApp
import com.senda.lecturabiblica.ui.SendaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            SendaTheme(state.themeMode, state.accent) {
                SendaApp(state, viewModel)
            }
        }
        intent?.data?.let(viewModel::requestRestore)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(viewModel::requestRestore)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppForeground()
    }
}
