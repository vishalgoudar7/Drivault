/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication

import android.os.Bundle
import android.widget.TextView
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.utils.DisplayUtils
import android.util.Log

class DeepLinkLoginActivity :
    AuthenticatorActivity(),
    Injectable {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e("DRIVAULT_LOGIN", "DeepLinkLoginActivity opened")
        Log.e("DRIVAULT_LOGIN", "URI = ${intent?.data}")
        if (!MDMConfig.multiAccountSupport(this) && accountManager.accounts.size == 1) {
            DisplayUtils.showSnackMessage(this, R.string.no_mutliple_accounts_allowed)
            return
        }

        setContentView(R.layout.deep_link_login)

        intent.data?.let {
            Log.e("DRIVAULT_LOGIN", "Processing URL = $it")
            try {
                val prefix = getString(R.string.login_data_own_scheme) + PROTOCOL_SUFFIX + "login/"
                Log.e("DRIVAULT_LOGIN", "Prefix = $prefix")
                val loginUrlInfo = parseLoginDataUrl(prefix, it.toString())
                Log.e("DRIVAULT_LOGIN", "Server = ${loginUrlInfo.server}")
                Log.e("DRIVAULT_LOGIN", "User = ${loginUrlInfo.loginName}")
                val loginText = findViewById<TextView>(R.id.loginInfo)
                loginText.text = String.format(
                    getString(R.string.direct_login_text),
                    loginUrlInfo.loginName,
                    loginUrlInfo.server
                )
            } catch (e: Exception) {
                Log.e("DRIVAULT_LOGIN", "Login error", e)
                DisplayUtils.showSnackMessage(this, R.string.direct_login_failed)
            }
        }
    }
}
