package com.flagsnerd.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flagsnerd.prism.data.Store
import com.flagsnerd.prism.update.Updater

@Composable
private fun CardTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Swatch(colors: List<Color>, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.bounceClick(onClick = onClick).padding(4.dp)
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(Brush.linearGradient(colors))
                .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White)
        }
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun SettingsScreen(bottomPad: Dp, onInstallUpdate: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPad),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { ScreenHeader("Settings", "Make it yours") }

        item {
            GlassCard(Modifier.staggerIn(1, "set1")) {
                CardTitle(Icons.Rounded.Palette, "Theme")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Swatch(
                            listOf(Color(0xFF4C61FF), Color(0xFFFF7AC6), Color(0xFFFFC857)),
                            "You", Store.palette == "you"
                        ) { Store.palette = "you" }
                    }
                    items(Palettes, key = { it.id }) { p ->
                        Swatch(
                            listOf(Color(p.container), Color(p.primary)),
                            p.label, Store.palette == p.id
                        ) { Store.palette = p.id }
                    }
                }
            }
        }

        item {
            var level by remember { mutableFloatStateOf(Store.glassLevel) }
            GlassCard(Modifier.staggerIn(2, "set2")) {
                CardTitle(Icons.Rounded.AutoAwesome, "Look and motion")
                Spacer(Modifier.height(8.dp))
                SwitchRow("Animated background", "Soft colour glow drifting behind the app", Store.aurora) { Store.aurora = it }
                SwitchRow("Animations", "Springy presses, screen transitions, entrances", Store.animations) { Store.animations = it }
                Text("Glass intensity", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 10.dp))
                Slider(
                    value = level,
                    onValueChange = { level = it },
                    onValueChangeFinished = { Store.glassLevel = level },
                    valueRange = 0.5f..1.6f
                )
                Box(
                    Modifier.fillMaxWidth().height(56.dp).glass(RoundedCornerShape(20.dp), 0.12f, level),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Liquid glass preview", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        item {
            GlassCard(Modifier.staggerIn(3, "set3")) {
                CardTitle(Icons.Rounded.Person, "Your name")
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = Store.userName,
                    onValueChange = { Store.userName = it },
                    placeholder = { Text("Shown in the greeting") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.White.copy(alpha = 0.10f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            GlassCard(Modifier.staggerIn(4, "set4")) {
                CardTitle(Icons.Rounded.PlayCircle, "Playback")
                Spacer(Modifier.height(6.dp))
                SwitchRow("Play next episode", "Continue with the next episode automatically", Store.autoNext) { Store.autoNext = it }
                SwitchRow("Open in landscape", "Rotate to landscape when a video starts", Store.autoLandscape) { Store.autoLandscape = it }
            }
        }

        item {
            GlassCard(Modifier.staggerIn(5, "set5")) {
                CardTitle(Icons.Rounded.SystemUpdate, "Updates")
                Text(
                    "Version ${Updater.installedName(ctx)}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )
                if (Updater.available != null) {
                    Button(onClick = onInstallUpdate) { Text("Download and install") }
                } else {
                    FilledTonalButton(onClick = { Updater.check(ctx) }, enabled = !Updater.checking) {
                        Text("Check for updates")
                    }
                }
                if (Updater.status.isNotEmpty()) {
                    Text(
                        Updater.status, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        item {
            GlassCard(Modifier.staggerIn(6, "set6")) {
                CardTitle(Icons.Rounded.Info, "About")
                Text(
                    "Prism plays your videos with libVLC. Unofficial, not affiliated with VideoLAN.\n\n" +
                        "The Retro Blendy display font is a personal-use demo by Sron Studio; buy a licence before sharing the app.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
