package com.weave.app
import android.content.Intent
import android.os.Bundle
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
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var visible by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(if (visible) 1f else 0.5f, tween(1000, easing = EaseOutBack), label = "s")
            val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(1000), label = "a")
            
            LaunchedEffect(Unit) {
                visible = true
                delay(1500)
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
            // Golden Cream Background
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8E1)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WEAVE", color = Color(0xFFFFB300), fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.scale(scale).alpha(alpha))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Developed by Arif Ali", color = Color(0xFF8D6E63), fontSize = 14.sp, modifier = Modifier.alpha(alpha))
                }
            }
        }
    }
}
