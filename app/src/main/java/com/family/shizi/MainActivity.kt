package com.family.shizi

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.family.shizi.navigation.ShiziNavHost
import com.family.shizi.ui.theme.ShiziTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val app = application as ShiziApplication

        if (app.databaseOpenFailed) {
            setContent {
                ShiziTheme {
                    DatabaseRecoveryScreen()
                }
            }
            return
        }

        setContent {
            ShiziTheme {
                ShiziNavHost()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            val app = application as ShiziApplication
            app.repository?.settleAllOpenActiveSegments()
        }
    }
}

@Composable
fun DatabaseRecoveryScreen() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "数据库损坏",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("db_recovery_title"),
            )
            Text(
                "学习数据无法读取，需要家长处理。",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp).testTag("db_recovery_message"),
            )
            Text(
                "您可以尝试清空数据恢复默认状态，或联系技术支持。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                // A broken database is deliberately never deleted from this screen. Recovery is
                // routed to the adult-gated parent flow after diagnostics are preserved.
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .testTag("db_recovery_clear"),
            ) { Text("请联系家长处理")
            }
        }
    }
}
