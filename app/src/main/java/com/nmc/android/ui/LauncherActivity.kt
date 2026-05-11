/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2023-2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.SettingsActivity
import javax.inject.Inject

class LauncherActivity : BaseActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (user.isPresent) {
            if (MDMConfig.enforceProtection(this) && appPreferences.lockPreference == SettingsActivity.LOCK_NONE) {
                startActivity(Intent(this, SettingsActivity::class.java))
            } else {
                startActivity(Intent(this, FileDisplayActivity::class.java))
            }
        } else {
            startActivity(Intent(this, AuthenticatorActivity::class.java))
        }

        finish()
    }
}
