/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Context
import nl.rijksoverheid.en.R

class InteropAnnouncementInfoItem(
    actionMoreInfo: () -> Unit,
    actionClose: () -> Unit
) : BaseStatusInfoItem() {

    override val viewState =
        object : InfoViewState(actionMoreInfo, actionClose) {
            override fun getMessage(context: Context) =
                context.getString(R.string.status_info_interop_announcement_message)
        }
}
