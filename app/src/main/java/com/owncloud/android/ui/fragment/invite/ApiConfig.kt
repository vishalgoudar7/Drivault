/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.invite

object ApiConfig {

    private const val IS_LOCAL = false

    private const val LOCAL_URL =
        "http://10.0.2.2/New%20folder/api/"

    private const val LIVE_URL =
        "http://103.174.148.208/api/"

    val BASE_URL =
        if (IS_LOCAL) LOCAL_URL else LIVE_URL
}