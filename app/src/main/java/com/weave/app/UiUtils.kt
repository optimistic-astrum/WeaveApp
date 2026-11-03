package com.weave.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Ye ek premium Shimmer (Loading) effect hai jo data load hote waqt chamkega
@Composable
fun ShimmerLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.width(100.dp).height(16.dp).background(Color(0xFF3A3A3A), RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.width(200.dp).height(12.dp).background(Color(0xFF3A3A3A), RoundedCornerShape(8.dp)))
        }
    }
}
