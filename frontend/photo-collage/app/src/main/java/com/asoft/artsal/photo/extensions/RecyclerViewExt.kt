package com.asoft.artsal.photo.extensions

import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

fun <T : RecyclerView.Adapter<*>> RecyclerView.initRecyclerViewAdapter(
    yourAdapter: T?,
    layoutOrientation: Int = RecyclerView.VERTICAL,
    fixedSize: Boolean = true,
    reverseLayout: Boolean = false
) {
    apply {
        layoutManager = LinearLayoutManager(context, layoutOrientation, reverseLayout)
        adapter = yourAdapter
        setHasFixedSize(fixedSize)
    }
}

fun RecyclerView.loadMoreFlow(): Flow<Int> = callbackFlow {
    checkMainThread()
    val listener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (!recyclerView.canScrollVertically(1)) {
                trySend(newState)
                Timber.d("LoadMore enable >>>> ")
            }

        }
    }
    addOnScrollListener(listener)
    awaitClose { removeOnScrollListener(listener) }
}.conflate()

fun RecyclerView.loadMoreHorizontalFlow(): Flow<Int> = callbackFlow {
    checkMainThread()
    val listener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (!recyclerView.canScrollHorizontally(1)) {
                trySend(newState)
                Timber.d("LoadMore enable >>>> ")
            }

        }
    }
    addOnScrollListener(listener)
    awaitClose { removeOnScrollListener(listener) }
}.conflate()

fun <T : RecyclerView.Adapter<*>> RecyclerView.initRecyclerViewAdapter(
    yourAdapter: T,
    yourLayoutManager: RecyclerView.LayoutManager,
    fixedSize: Boolean = true
) {
    apply {
        layoutManager = yourLayoutManager
        adapter = yourAdapter
        setHasFixedSize(fixedSize)
    }
}

suspend fun <T> ListAdapter<T, *>.submitListSuspend(list: List<T>?) =
    suspendCancellableCoroutine<Unit> { cont ->
        submitList(list) {
            cont.resume(Unit)
        }
    }

fun <T, VH : RecyclerView.ViewHolder> ListAdapter<T, VH>.updateListContent(list: List<T>?) {
    // ListAdapter<>.submitList() contains (stripped):
    //  if (newList == mList) {
    //      // nothing to do
    //      return;
    //  }
    this.submitList(if (list == this.currentList) list.toList() else list)
}
suspend fun RecyclerView.awaitViewHolder(position: Int): RecyclerView.ViewHolder {
    val adapter = checkNotNull(adapter) {
        "Tried to get ViewHolder at position $position, but the adapter was null"
    }

    check(adapter.itemCount > 0) {
        "Tried to get ViewHolder at position $position, but the list was empty"
    }

    var viewHolder: RecyclerView.ViewHolder?


    do {
        viewHolder = findViewHolderForAdapterPosition(position)
        Timber.tag("###").d("position=$position, viewHolder = $viewHolder")
    } while (
        currentCoroutineContext().isActive
        && viewHolder == null
        && awaitNextLayout() == Unit
    )

    Timber.tag("###").d("DONE position=$position, viewHolder = $viewHolder")
    return requireNotNull(viewHolder)
}

fun RecyclerView.cleanup() {
    adapter = null
}

fun RecyclerView.scrollInNestedScrollView() {
    addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> rv.parent
                    .requestDisallowInterceptTouchEvent(true)
            }
            return false
        }

        override fun onTouchEvent(view: RecyclerView, event: MotionEvent) {}
        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    })
}

fun RecyclerView.findViewHolderByViewType(viewType: Int): RecyclerView.ViewHolder? {
    try {
        for (i in 0 until this.childCount) {
            val child = this.getChildAt(i) ?: continue
            val viewHolder = this.getChildViewHolder(child)

            if (this.adapter?.getItemViewType(viewHolder.adapterPosition) == viewType) {
                return viewHolder
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return null // No matching ViewHolder found
}