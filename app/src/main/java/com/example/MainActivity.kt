package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.KaliDesktopScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SystemViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var isSystemOn by remember { mutableStateOf(true) }
        val viewModel: SystemViewModel = viewModel()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          if (isSystemOn) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ) {
              KaliDesktopScreen(
                viewModel = viewModel,
                onShutdown = { isSystemOn = false }
              )
            }
          } else {
            // High-fidelity shutdown terminal splash screen
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Text(
                  text = "[SYSTEM shutdown completed]\n\nINIT: Switching to runlevel 0\nINIT: Sending all processes prim signals\nSending SIGTERM to remaining processes... Done.\nSending SIGKILL to remaining processes... Done.\nUnmounting virtual databases (/home/kali/db)... Done.\nDeconfiguring loopback NAT adapters... Done.\nPowering off virtual terminal CPU.\n\n======================================\n✨ SYSTEM HALTED SAFELY ✨\n======================================",
                  color = Color(0xFF00FF66),
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp,
                  lineHeight = 16.sp,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                  onClick = {
                    isSystemOn = true
                    viewModel.activeWindows.clear()
                    viewModel.executeTerminalCommand("clear")
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F80FF))
                ) {
                  Text(
                    text = "REBOOT MINI PC SESSION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
