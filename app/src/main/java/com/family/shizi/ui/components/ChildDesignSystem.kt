package com.family.shizi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.family.shizi.ui.theme.CardWhite
import com.family.shizi.ui.theme.ChallengePurple
import com.family.shizi.ui.theme.ShiziShapes
import com.family.shizi.ui.theme.ShiziSpacing
import com.family.shizi.ui.theme.StarYellow

@Composable
fun ChildPage(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().padding(horizontal = ShiziSpacing.page, vertical = 12.dp)) { content() }
    }
}

@Composable
fun ChildTopBar(title: String, onBack: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            OutlinedButton(onClick = onBack, modifier = Modifier.size(56.dp), contentPadding = ButtonDefaults.ContentPadding) {
                Text("返回", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun MascotBubble(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = ShiziShapes.card, color = CardWhite, tonalElevation = 2.dp) {
        Text(text, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun CharacterHeroCard(character: String, pinyin: String, modifier: Modifier = Modifier, onReplay: () -> Unit) {
    Card(modifier.fillMaxWidth(), shape = ShiziShapes.card, colors = CardDefaults.cardColors(containerColor = CardWhite)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(character, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
            Text(pinyin, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            AudioReplayButton(onClick = onReplay)
        }
    }
}

@Composable
fun AudioReplayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(56.dp), shape = ShiziShapes.button) { Text("再听一次") }
}

@Composable
fun ChildPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(64.dp), shape = ShiziShapes.button) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ProgressDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            Surface(Modifier.size(if (index == current) 14.dp else 10.dp), shape = CircleShape,
                color = if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant) {}
        }
    }
}

@Composable
fun PictureOptionCard(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    OptionCard(label, onClick, modifier, selected, MaterialTheme.colorScheme.secondaryContainer)
}

@Composable
fun CharacterOptionCard(character: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    OptionCard(character, onClick, modifier, selected, MaterialTheme.colorScheme.primaryContainer, large = true)
}

@Composable
private fun OptionCard(label: String, onClick: () -> Unit, modifier: Modifier, selected: Boolean, color: Color, large: Boolean = false) {
    Card(modifier.fillMaxWidth().height(112.dp).clip(ShiziShapes.card).clickable(role = Role.Button, onClick = onClick).semantics { role = Role.Button },
        shape = ShiziShapes.card, border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = if (selected) color else CardWhite)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = if (large) MaterialTheme.typography.displayMedium else MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun FeedbackBanner(text: String, positive: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = ShiziShapes.card, color = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun StarReward(stars: Int, modifier: Modifier = Modifier) {
    Surface(modifier, shape = ShiziShapes.pill, color = StarYellow) {
        Text("成长星星 +$stars", Modifier.padding(horizontal = 18.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BadgeCard(title: String, detail: String, unlocked: Boolean, modifier: Modifier = Modifier) {
    Card(modifier, shape = ShiziShapes.card, colors = CardDefaults.cardColors(containerColor = if (unlocked) StarYellow.copy(alpha = .35f) else CardWhite)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (unlocked) ChallengePurple else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (unlocked) "已点亮" else detail, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AvatarCard(name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.size(96.dp).clickable(onClick = onClick), shape = CircleShape,
        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name, style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
fun EmptyState(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(detail, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}
