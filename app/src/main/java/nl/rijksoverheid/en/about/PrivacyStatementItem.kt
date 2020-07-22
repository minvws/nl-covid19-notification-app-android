/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.about

import com.xwray.groupie.Item
import nl.rijksoverheid.en.R

class PrivacyStatementItem : AboutFAQItem(R.string.about_privacy_statement) {
    override fun isSameAs(other: Item<*>): Boolean = other is PrivacyStatementItem
    override fun hasSameContentAs(other: Item<*>) = other is PrivacyStatementItem
}
