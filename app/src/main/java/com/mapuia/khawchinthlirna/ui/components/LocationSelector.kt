package com.mapuia.khawchinthlirna.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapuia.khawchinthlirna.data.FocusLocation
import com.mapuia.khawchinthlirna.data.FocusLocations
import com.mapuia.khawchinthlirna.data.preferences.SelectedLocationMode

private val LocationAccentBlue = Color(0xFF60A5FA)
private val LocationAccentBlueSoft = Color(0xFFBFDBFE)

@Composable
fun LocationSwitcherCard(
    selectedLabel: String,
    selectedMode: SelectedLocationMode,
    isMizo: Boolean,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (isMizo) "Hmun thlan" else "Location"
    val subtitle = if (selectedMode == SelectedLocationMode.CURRENT) {
        if (isMizo) "GPS hmanga hmun chhut" else "Using GPS location"
    } else {
        if (isMizo) "Manual thlan" else "Selected manually"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPicker),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            LocationAccentBlue.copy(alpha = 0.14f),
                            Color(0xFF06B6D4).copy(alpha = 0.08f),
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (selectedMode == SelectedLocationMode.CURRENT) Icons.Default.MyLocation else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = LocationAccentBlueSoft,
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f),
                    )
                }
            }
            Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = if (isMizo) "Thlan" else "Change",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = LocationAccentBlueSoft,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    selectedGridId: String?,
    selectedMode: SelectedLocationMode,
    isMizo: Boolean,
    onDismiss: () -> Unit,
    onChooseCurrentLocation: () -> Unit,
    onChooseLocation: (FocusLocation) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, isMizo) { FocusLocations.search(query, isMizo) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = if (isMizo) "District / khua / POI thlang rawh" else "Choose district, city, or POI",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (isMizo) {
                    "Current Location chu default a ni. Search hmangin hmun dang pawh i thlang thei ang."
                } else {
                    "Current Location stays as default, but you can switch with search anytime."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = {
                    Text(if (isMizo) "Search district / khua" else "Search district / city")
                },
            )

            Spacer(modifier = Modifier.height(14.dp))

            LocationPickerRow(
                title = if (isMizo) "Current Location" else "Current Location",
                subtitle = if (isMizo) "GPS hmun hmang tur" else "Use GPS-based location",
                isSelected = selectedMode == SelectedLocationMode.CURRENT,
                isMizo = isMizo,
                onClick = onChooseCurrentLocation,
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(results, key = { it.id }) { location ->
                    LocationPickerRow(
                        title = if (isMizo) location.nameMz else location.name,
                        subtitle = location.category,
                        isSelected = selectedMode == SelectedLocationMode.MANUAL && selectedGridId == location.gridId,
                        isMizo = isMizo,
                        onClick = { onChooseLocation(location) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationPickerRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isMizo: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isSelected) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = if (isMizo) "Thlan tawh" else "Selected",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
