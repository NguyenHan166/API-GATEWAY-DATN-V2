package com.asoft.artsal.photo.extensions

import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.EditText
import androidx.annotation.CheckResult
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import androidx.core.net.toUri

fun TextView.setSpannableColor(result: String, color: Int, start: Int, end: Int) {
    val builder = SpannableStringBuilder()
    builder.append(result)
    builder.setSpan(
        ForegroundColorSpan(color), start, end,
        Spanned.SPAN_INCLUSIVE_INCLUSIVE
    )
    text = builder
}

fun Context.getSpannableColor(
    result: String,
    color: Int,
    start: Int,
    end: Int
): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    builder.append(result)
    builder.setSpan(
        ForegroundColorSpan(color), start, end,
        Spanned.SPAN_INCLUSIVE_INCLUSIVE
    )
    return builder
}

fun Context.getSpannableColorAndLink(
    result: String,
    color: Int,
    start: Int,
    end: Int,
    privacyStart: Int,
    privacyEnd: Int,
    privacyUrl: String
): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    builder.append(result)

    builder.setSpan(
        ForegroundColorSpan(color),
        start,
        end,
        Spanned.SPAN_INCLUSIVE_INCLUSIVE
    )

    builder.setSpan(
        UnderlineSpan(),
        privacyStart,
        privacyEnd,
        Spanned.SPAN_INCLUSIVE_INCLUSIVE
    )
    builder.setSpan(object : ClickableSpan() {
        override fun onClick(widget: View) {
            val intent = Intent(Intent.ACTION_VIEW, privacyUrl.toUri())
            startActivity(intent)
        }
    }, privacyStart, privacyEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

    return builder
}

@ExperimentalCoroutinesApi
@CheckResult
fun EditText.textChanges(): Flow<CharSequence?> {
    return callbackFlow {
        checkMainThread()
        val listener = doOnTextChanged { text, _, _, _ -> trySend(text) }
        awaitClose { removeTextChangedListener(listener) }
    }.onStart { emit(text) }
}

fun TextView.makeLinks(vararg links: Pair<String, View.OnClickListener>) {
    val spannableString = SpannableString(this.text)
    var startIndexOfLink = -1
    for (link in links) {
        val clickableSpan = object : ClickableSpan() {
            override fun updateDrawState(textPaint: TextPaint) {
                // use this to change the link color
                textPaint.color = textPaint.linkColor
                // toggle below value to enable/disable
                // the underline shown below the clickable text
                textPaint.isUnderlineText = false
            }

            override fun onClick(view: View) {
                Selection.setSelection((view as TextView).text as Spannable, 0)
                view.invalidate()
                link.second.onClick(view)
            }
        }
        startIndexOfLink = this.text.toString().indexOf(link.first, startIndexOfLink + 1)
        if(startIndexOfLink == -1) continue // todo if you want to verify your texts contains links text
        spannableString.setSpan(
            clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    this.movementMethod =
        LinkMovementMethod.getInstance() // without LinkMovementMethod, link can not click
    this.setText(spannableString, TextView.BufferType.SPANNABLE)
}

fun TextView.startDrawable(@DrawableRes id: Int = 0, @DimenRes sizeRes: Int) {
    if (id != 0) {
        val drawable = ContextCompat.getDrawable(context, id)
        val size = resources.getDimensionPixelSize(sizeRes)
        drawable?.setBounds(0, 0, size, size)
        this.setCompoundDrawables(drawable, null, null, null)
    } else {
        this.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }
}

fun TextView.endDrawable(@DrawableRes id: Int = 0, @DimenRes sizeRes: Int) {
    if (id != 0) {
        val drawable = ContextCompat.getDrawable(context, id)
        val size = resources.getDimensionPixelSize(sizeRes)
        drawable?.setBounds(0, 0, size, size)
        this.setCompoundDrawables(null, null, drawable, null)
    } else {
        this.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }
}