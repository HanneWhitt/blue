// ReorderableState.kt
package com.example.blue.presentation

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DragDropState(
    val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set

    private var draggingItemInitialOffset by mutableStateOf(0f)
    private var draggingItemCurrentOffset by mutableStateOf(0f)

    val draggingItemOffset: Float
        get() = draggingItemCurrentOffset - draggingItemInitialOffset

    private val LazyListItemInfo.offsetEnd: Int
        get() = offset + size

    fun onDragStart(key: Any) {
        draggingItemKey = key
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        draggingItemInitialOffset = item?.offset?.toFloat() ?: 0f
        draggingItemCurrentOffset = draggingItemInitialOffset
    }

    fun getCurrentDraggingIndex(): Int? {
        return listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingItemKey }?.index
    }

    fun onDrag(offset: Offset) {
        draggingItemCurrentOffset += offset.y

        val draggingItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingItemKey }
        val targetItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            draggingItemKey?.let { dragKey ->
                item.key != dragKey &&
                item.key is Int &&  // Only target items with numeric keys (habit IDs)
                draggingItemCurrentOffset.toInt() in item.offset..item.offsetEnd
            } ?: false
        }

        if (draggingItem != null && targetItem != null) {
            // Adjust indices to account for header item at position 0
            val fromIndex = draggingItem.index - 1  // Subtract 1 for header
            val toIndex = targetItem.index - 1      // Subtract 1 for header

            if (fromIndex >= 0 && toIndex >= 0) {
                onMove(fromIndex, toIndex)
                // draggingItemKey stays the same - it follows the item

                // Update offsets
                val targetOffset = targetItem.offset.toFloat()
                draggingItemInitialOffset = targetOffset
                draggingItemCurrentOffset = targetOffset + draggingItemOffset
            }
        }

        // Auto-scroll
        val overscroll = when {
            draggingItemCurrentOffset < listState.layoutInfo.viewportStartOffset + 50 -> -10f
            draggingItemCurrentOffset > listState.layoutInfo.viewportEndOffset - 50 -> 10f
            else -> 0f
        }
        if (overscroll != 0f) {
            scope.launch {
                listState.scrollBy(overscroll)
            }
        }
    }

    fun onDragEnd() {
        draggingItemKey = null
        draggingItemInitialOffset = 0f
        draggingItemCurrentOffset = 0f
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (Int, Int) -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()
    return remember(lazyListState) {
        DragDropState(
            listState = lazyListState,
            scope = scope,
            onMove = onMove
        )
    }
}

fun Modifier.dragContainer(
    dragDropState: DragDropState,
    onDragStart: () -> Unit = {}
): Modifier {
    return this.pointerInput(dragDropState) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                onDragStart()
            },
            onDrag = { change, offset ->
                change.consume()
                dragDropState.onDrag(offset)
            },
            onDragEnd = {
                dragDropState.onDragEnd()
            },
            onDragCancel = {
                dragDropState.onDragEnd()
            }
        )
    }
}
