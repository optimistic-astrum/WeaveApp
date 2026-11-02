package com.weave.app
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var visible by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.5f, animationSpec = tween(1000, easing = EaseOutBack), label = "scale")
            val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(1000), label = "alpha")
            LaunchedEffect(Unit) {
                visible = true
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }, 2000)
            }
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WEAVE", color = Color(0xFF00E5FF), fontSize = 48.sp, fontWeight = FontWeight.Bold, modifier = Modifier.scale(scale).alpha(alpha))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Developed by Arif Ali", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.alpha(alpha))
                }
            }
        }
    }
}
