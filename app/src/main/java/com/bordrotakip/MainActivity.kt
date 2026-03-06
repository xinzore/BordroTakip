package com.bordrotakip

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bordrotakip.notification.NotificationIntents
import com.bordrotakip.ui.navigation.BordroNavHost
import com.bordrotakip.ui.theme.BordroTakipTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var openShiftPromptEpochDay: Long? by mutableStateOf(null)
    private var openSalaryPrediction: Boolean by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            BordroTakipTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BordroNavHost(
                        openShiftPromptEpochDay = openShiftPromptEpochDay,
                        onConsumeOpenShiftPrompt = { openShiftPromptEpochDay = null },
                        openSalaryPrediction = openSalaryPrediction,
                        onConsumeOpenSalaryPrediction = { openSalaryPrediction = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val shouldOpenSalaryPrediction = intent.getBooleanExtra(
            NotificationIntents.EXTRA_OPEN_SALARY_PREDICTION,
            false
        )
        if (shouldOpenSalaryPrediction) {
            openSalaryPrediction = true
            intent.removeExtra(NotificationIntents.EXTRA_OPEN_SALARY_PREDICTION)
        }

        val epochDay = intent.getLongExtra(
            NotificationIntents.EXTRA_OPEN_SHIFT_PROMPT_EPOCH_DAY,
            Long.MIN_VALUE
        )
        if (epochDay != Long.MIN_VALUE) {
            openShiftPromptEpochDay = epochDay
            intent.removeExtra(NotificationIntents.EXTRA_OPEN_SHIFT_PROMPT_EPOCH_DAY)
        }

        setIntent(intent)
    }
}
