package org.kaorun.nouto.ui.components

import android.util.TypedValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kaorun.nouto.R

@Composable
fun getThemeColor(attr: Int): Color {
    val context = LocalContext.current
    val typedValue = remember { TypedValue() }
    context.theme.resolveAttribute(attr, typedValue, true)
    return Color(typedValue.data)
}

@Composable
fun MorphingFab(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    text: String = "Create"
) {
    var renderGate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { renderGate = true }

    if (!renderGate) {
        Spacer(modifier = Modifier.size(56.dp))
        return
    }

    val transition = updateTransition(targetState = isExpanded, label = "fab_transition")

    val fabColor = getThemeColor(com.google.android.material.R.attr.colorPrimaryContainer)
    val contentColor = getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer)

    val fabWidth by transition.animateDp(
        label = "fab_width",
        transitionSpec = { tween(durationMillis = 250) }
    ) { if (it) 80.dp else 180.dp }

    val cornerRadius = 20.dp

    Surface(
        modifier = Modifier.size(width = fabWidth, height = 80.dp),
        shape = RoundedCornerShape(cornerRadius),
        color = fabColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onToggle
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isExpanded) {
                Icon(
                    painter = painterResource(id = R.drawable.close_24px),
                    contentDescription = "Close",
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.edit_24px),
                        contentDescription = "Create",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoutoFabMenu(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreateNote: () -> Unit,
    onCreateFolder: () -> Unit,
    onImportFiles: () -> Unit,
    onAddLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subFabColor = getThemeColor(com.google.android.material.R.attr.colorSecondaryContainer)
    val subFabContentColor = getThemeColor(com.google.android.material.R.attr.colorOnSecondaryContainer)

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = isExpanded,
        button = {
            MorphingFab(
                isExpanded = isExpanded,
                onToggle = { onExpandedChange(!isExpanded) }
            )
        }
    ) {
        FloatingActionButtonMenuItem(
            modifier = Modifier.height(76.dp),
            onClick = {
                onExpandedChange(false)
                onCreateNote()
            },
            icon = { Icon(painterResource(id = R.drawable.edit_24px), contentDescription = null, modifier = Modifier.size(28.dp)) },
            text = { Text("Create Note", fontSize = 18.sp) },
            containerColor = subFabColor,
            contentColor = subFabContentColor
        )

        FloatingActionButtonMenuItem(
            modifier = Modifier.height(76.dp),
            onClick = {
                onExpandedChange(false)
                onCreateFolder()
            },
            icon = { Icon(painterResource(id = R.drawable.folder_24px), contentDescription = null, modifier = Modifier.size(28.dp)) },
            text = { Text("Create Folder", fontSize = 18.sp) },
            containerColor = subFabColor,
            contentColor = subFabContentColor
        )

        FloatingActionButtonMenuItem(
            modifier = Modifier.height(76.dp),
            onClick = {
                onExpandedChange(false)
                onImportFiles()
            },
            icon = { Icon(painterResource(id = R.drawable.upload_24px), contentDescription = null, modifier = Modifier.size(28.dp)) },
            text = { Text("Import Files", fontSize = 18.sp) },
            containerColor = subFabColor,
            contentColor = subFabContentColor
        )

        FloatingActionButtonMenuItem(
            modifier = Modifier.height(76.dp),
            onClick = {
                onExpandedChange(false)
                onAddLink()
            },
            icon = { Icon(painterResource(id = R.drawable.link_24px), contentDescription = null, modifier = Modifier.size(28.dp)) },
            text = { Text("Add Link", fontSize = 18.sp) },
            containerColor = subFabColor,
            contentColor = subFabContentColor
        )
    }
}
