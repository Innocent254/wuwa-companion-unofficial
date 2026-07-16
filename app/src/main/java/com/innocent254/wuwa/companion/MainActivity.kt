package com.innocent254.wuwa.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.innocent254.wuwa.companion.ui.theme.WuWaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WuWaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StarterScreen()
                }
            }
        }
    }
}

@Composable
private fun StarterScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("WuWa Companion", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Android client foundation is ready. Database and app updates are checked independently.",
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
