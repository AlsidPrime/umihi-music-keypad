package ca.ilianokokoro.umihi.music.ui.components.keypad

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun isKeypadScreen(): Boolean {
    val size = LocalConfiguration.current
    return minOf(size.screenWidthDp, size.screenHeightDp) <= 360 &&
        maxOf(size.screenWidthDp, size.screenHeightDp) <= 480
}

internal class KeypadNavigationFocus {
    val navigation = FocusRequester()
    var content by mutableStateOf<FocusRequester?>(null)
}

internal val LocalKeypadNavigationFocus = staticCompositionLocalOf<KeypadNavigationFocus?> { null }

internal data class KeypadEntry(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val enabled: Boolean = true,
    val onOptions: (() -> Unit)? = null,
    val onClick: () -> Unit,
)

/** One focused list owns its cursor, including rows not currently composed. */
@Composable
internal fun KeypadList(
    entries: List<KeypadEntry>,
    modifier: Modifier = Modifier,
    initialKey: String? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    autoFocus: Boolean = true,
    registerNavigation: Boolean = true,
    onBoundary: (() -> Unit)? = null,
    onLeft: (() -> Unit)? = null,
    followKey: String? = null,
) {
    val navigation = if (registerNavigation) LocalKeypadNavigationFocus.current else null
    val listState = rememberLazyListState()
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var hasFocus by remember { mutableStateOf(false) }
    val keys = entries.map { it.key }
    val enabled = entries.map { it.enabled }
    val selectedIndex = selectedEntryIndex(keys, enabled, selectedKey, initialKey)
    val windowFocused = LocalWindowInfo.current.isWindowFocused

    DisposableEffect(navigation, focusRequester) {
        navigation?.content = focusRequester
        onDispose {
            navigation?.let { if (it.content === focusRequester) it.content = null }
        }
    }
    LaunchedEffect(windowFocused, autoFocus) {
        if (windowFocused && autoFocus) focusRequester.requestFocus()
    }
    LaunchedEffect(keys, enabled) {
        if (selectedKey !in keys || selectedIndex?.let { keys[it] } != selectedKey) {
            selectedKey = selectedIndex?.let { keys[it] }
        }
    }
    LaunchedEffect(followKey, hasFocus) {
        if (!hasFocus && followKey != null && followKey in keys) selectedKey = followKey
    }
    LaunchedEffect(selectedIndex, windowFocused) {
        val index = selectedIndex ?: return@LaunchedEffect
        // Avoid moving the viewport when the selected row is already fully visible.
        val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (visible == null || visible.offset < listState.layoutInfo.viewportStartOffset ||
            visible.offset + visible.size > listState.layoutInfo.viewportEndOffset) {
            listState.scrollToItem(index)
        }
    }

    fun leaveList() {
        if (onBoundary != null) onBoundary() else navigation?.navigation?.requestFocus()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                val code = key.keyCode
                val recognized = code in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)
                if (!recognized || !key.hasNoModifiers()) return@onPreviewKeyEvent false
                if (key.action == KeyEvent.ACTION_DOWN) {
                    when (code) {
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                            val next = moveEntrySelection(enabled, selectedIndex,
                                if (code == KeyEvent.KEYCODE_DPAD_UP) -1 else 1)
                            if (next == null) leaveList() else selectedKey = keys[next]
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> if (key.repeatCount == 0) {
                            if (onLeft != null) onLeft() else leaveList()
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> if (key.repeatCount == 0) {
                            val options = selectedIndex?.let { entries[it].onOptions }
                            if (options != null) options() else leaveList()
                        }
                        else -> if (key.repeatCount == 0) selectedIndex?.let { entries[it].onClick() }
                    }
                }
                true
            }
            .focusRequester(focusRequester)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable(),
    ) {
        itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
            val active = hasFocus && selectedIndex == index
            val shape = RoundedCornerShape(6.dp)
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                    .border(if (active) 3.dp else 1.dp,
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
                    .onFocusChanged { if (it.isFocused && entry.enabled) selectedKey = entry.key }
                    .semantics { selected = active }
                    .clickable(enabled = entry.enabled, onClick = {
                        selectedKey = entry.key
                        entry.onClick()
                    })
                    .padding(8.dp),
            ) {
                Text(entry.title, style = MaterialTheme.typography.labelMedium,
                    color = if (entry.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                entry.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
