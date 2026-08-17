package tv.own.owntv.ui.components

import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * Full-screen overlay for manually reordering items via D-pad Up/Down.
 * Intercepts all D-pad/OK/Back events while active; the main screen's paging list is
 * hidden underneath. Commit (OK) saves; Back cancels.
 */
@Composable
fun MoveOrderOverlay(
    title: String,
    itemNames: List<String>,
    activeIndex: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Retry several times: the Live TV screen's preview-pane buttons can briefly steal focus
    // between the context menu closing and moveState being set. Retries are no-ops when already focused.
    LaunchedEffect(Unit) {
        repeat(6) {
            delay(50L * (it + 1)) // 50, 100, 150, 200, 250, 300 ms
            runCatching { focus.requestFocus() }
        }
    }
    LaunchedEffect(activeIndex) {
        if (itemNames.isNotEmpty()) listState.animateScrollToItem(activeIndex.coerceIn(0, itemNames.lastIndex))
    }
    androidx.activity.compose.BackHandler { onCancel() }

    // Outer Box intercepts D-pad via onKeyEvent when any child has focus.
    // NOT focusable() itself — focus lives on the Save button so TV focus finds it.
    // trapAllFocusExit cancels every directional exit so a stray Left/Right (not handled by
    // onKeyEvent below, which only routes Up/Down/Back) can't escape the overlay onto the screen
    // behind it.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .trapAllFocusExit()
            .focusGroup()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { onMoveUp(); true }
                    Key.DirectionDown -> { onMoveDown(); true }
                    Key.Back -> { onCancel(); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(Dimens.CardCorner))
                .background(colors.surfaceContainerHigh)
                .padding(Dimens.GapLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(
                stringResource(R.string.common_move_instructions),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Dimens.GapTiny))
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny),
            ) {
                itemsIndexed(itemNames) { idx, name ->
                    val isActive = idx == activeIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CornerXSmall))
                            .background(if (isActive) colors.primary else colors.surfaceContainerLowest)
                            .padding(horizontal = Dimens.HeroGap, vertical = Dimens.GapCompact),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isActive) {
                                Text(stringResource(R.string.setup_move_indicator) + " ", style = MaterialTheme.typography.bodyMedium, color = colors.onPrimary)
                            }
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isActive) colors.onPrimary else colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GapCompact), modifier = Modifier.fillMaxWidth()) {
                // Save gets the focus: TV focus lands here on overlay open, so onKeyEvent fires.
                OwnTVButton(stringResource(R.string.common_save), onClick = onCommit, modifier = Modifier.weight(1f).focusRequester(focus))
                OwnTVButton(stringResource(R.string.common_cancel), onClick = onCancel, style = OwnTVButtonStyle.SECONDARY, modifier = Modifier.weight(1f))
            }
        }
    }
}
