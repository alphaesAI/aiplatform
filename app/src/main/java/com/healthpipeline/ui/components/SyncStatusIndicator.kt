package com.healthpipeline.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthpipeline.ui.theme.NothingRed

enum class SyncStatus {
    SYNCED, SYNCING, ERROR, PENDING
}

@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    lastSyncTime: String? = null,
    modifier: Modifier = Modifier
) {
    val (icon, text, color) = when (status) {
        SyncStatus.SYNCED -> Triple(
            Icons.Default.CheckCircle,
            "SYNCED",
            MaterialTheme.colorScheme.onSurface
        )
        SyncStatus.SYNCING -> Triple(
            Icons.Default.Refresh,
            "SYNCING",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        SyncStatus.ERROR -> Triple(
            Icons.Default.Close,
            "ERROR",
            NothingRed
        )
        SyncStatus.PENDING -> Triple(
            Icons.Default.Info,
            "PENDING",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(2.dp)
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
            lastSyncTime?.let {
                Text(
                    text = "• $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
