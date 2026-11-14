package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.FinanceRepository
import com.example.ui.FinanceViewModel
import com.example.ui.FinanceViewModelFactory
import com.example.ui.screens.MainAppScreen
import com.example.ui.screens.SoundEffectManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.data.FirebaseUserManager.init(applicationContext)
        com.example.ocr.OcrConfigManager.init(applicationContext)
        com.example.util.AnnouncementSyncScheduler.startAllBackgroundSync(applicationContext)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = FinanceRepository(database.transactionDao(), database.preferenceDao(), database.announcementDao())

        // Check if launched via 'Fix Now' notification
        val actionApplyHotFix = intent?.getBooleanExtra("ACTION_APPLY_HOT_FIX", false) ?: false
        if (actionApplyHotFix) {
            val prompt = intent?.getStringExtra("HOT_FIX_PROMPT")
            val ver = intent?.getIntExtra("HOT_FIX_VERSION", 1) ?: 1
            if (!prompt.isNullOrBlank()) {
                com.example.ocr.OcrConfigManager.applyHotFix(applicationContext, prompt, ver)
            } else {
                com.example.ocr.OcrConfigManager.applyPendingHotFix(applicationContext)
            }
            com.example.util.AppNotificationHelper.cancelOcrBugFixNotification(applicationContext)
        }

        val initialUpiPayload = intent?.data?.toString()

        setContent {
            val viewModel: FinanceViewModel = viewModel(
                factory = FinanceViewModelFactory(repository)
            )
            
            LaunchedEffect(initialUpiPayload) {
                if (initialUpiPayload != null) {
                    viewModel.setIncomingUpiPayload(initialUpiPayload)
                }
            }

            MainAppScreen(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.data?.toString()?.let { payload ->
            FinanceViewModel.setDeepLinkPayload(payload)
        }
        val actionApplyHotFix = intent.getBooleanExtra("ACTION_APPLY_HOT_FIX", false)
        if (actionApplyHotFix) {
            val prompt = intent.getStringExtra("HOT_FIX_PROMPT")
            val ver = intent.getIntExtra("HOT_FIX_VERSION", 1)
            if (!prompt.isNullOrBlank()) {
                com.example.ocr.OcrConfigManager.applyHotFix(applicationContext, prompt, ver)
            } else {
                com.example.ocr.OcrConfigManager.applyPendingHotFix(applicationContext)
            }
            com.example.util.AppNotificationHelper.cancelOcrBugFixNotification(applicationContext)
        }
    }
}
