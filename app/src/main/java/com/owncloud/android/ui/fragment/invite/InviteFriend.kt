/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.invite

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InviteFriend(
    val name: String,
    val email: String,
    val mobile: String,
    val isAccepted: Boolean = false
) : Parcelable
