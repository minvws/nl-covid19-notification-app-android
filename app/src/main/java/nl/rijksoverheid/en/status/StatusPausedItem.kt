/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.status

import android.os.CountDownTimer
import android.text.format.DateFormat
import androidx.core.text.HtmlCompat
import com.xwray.groupie.viewbinding.GroupieViewHolder
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.ItemStatusPausedBinding
import nl.rijksoverheid.en.items.BaseBindableItem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class StatusPausedItem(
    private val pausedUntil: LocalDateTime,
    private val action: () -> Unit
) : BaseBindableItem<ItemStatusPausedBinding>() {

    data class ViewState(
        var message: CharSequence,
        val action: () -> Unit,
    )

    private var timer: CountDownTimer? = null

    override fun bind(viewBinding: ItemStatusPausedBinding, position: Int) {
        val message = viewBinding.root.context?.let { context ->
            if (pausedUntil.isAfter(LocalDateTime.now())) {
                val format = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
                HtmlCompat.fromHtml(
                    context.getString(
                        R.string.status_en_api_paused_description,
                        DateTimeFormatter.ofPattern(format, Locale.getDefault())
                            .format(pausedUntil)
                    ),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            } else {
                context.getString(R.string.status_en_api_paused_duration_reached)
            }
        } ?: return
        viewBinding.viewState = ViewState(message, action)
    }

    override fun getLayout() = R.layout.item_status_paused

    override fun onViewDetachedFromWindow(viewHolder: GroupieViewHolder<ItemStatusPausedBinding>) {
        timer?.cancel()
        super.onViewDetachedFromWindow(viewHolder)
    }
}