/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.invite
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentInviteFriendsBinding
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import okhttp3.Call
import okhttp3.Callback
import androidx.core.content.edit
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import android.accounts.AccountManager
import android.content.Intent
import android.graphics.Color
import com.google.android.material.snackbar.Snackbar
// import android.widget.ImageView
// import android.widget.LinearLayout
// import android.widget.Toast

import android.widget.LinearLayout
import androidx.core.net.toUri
import org.json.JSONArray
import org.json.JSONObject


// import com.google.android.material.snackbar.Snackbar


class InviteFriendsFragment : Fragment() {

    private var binding: FragmentInviteFriendsBinding? = null
    private var openedActionLayout: View? = null
    private val invitedFriends = mutableListOf<InviteFriend>()

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private val client by lazy {
        OkHttpClient()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInviteFriendsBinding.inflate(inflater, container, false)
        return requireNotNull(binding).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restoreInvitedFriends(savedInstanceState)
        setupViews()
        setupClickListeners()
        renderInvitedFriends()

// NEW API CALL
        fetchInvitedUsers()
        // Close menu when touching outside
        view.setOnClickListener {

            openedActionLayout?.let {

                (it.parent as? LinearLayout)?.removeView(it)

                openedActionLayout = null
            }
        }
    }

    private fun setupViews() {

        val binding = requireNotNull(binding)

        viewThemeUtils.material.run {

            colorMaterialButtonPrimaryFilled(binding.inviteFriendsAction)
            colorMaterialButtonPrimaryFilled(binding.inviteFriendsSend)

            colorTextInputLayout(binding.inviteFriendsNameContainer)
            colorTextInputLayout(binding.inviteFriendsEmailContainer)
            colorTextInputLayout(binding.inviteFriendsMobileContainer)
        }
    }

    private fun restoreInvitedFriends(savedInstanceState: Bundle?) {

        val restoredInvites =
            savedInstanceState?.getParcelableArrayList<InviteFriend>(
                KEY_INVITED_FRIENDS
            ).orEmpty()

        invitedFriends.clear()
        invitedFriends.addAll(restoredInvites)
    }

    private fun setupClickListeners() {

        val binding = requireNotNull(binding)

        binding.inviteFriendsAction.setOnClickListener {

            binding.inviteFriendsAction.setVisibleIf(false)
            binding.inviteFriendsForm.setVisibleIf(true)

            binding.inviteFriendsName.requestFocus()
        }

        binding.inviteFriendsSend.setOnClickListener {

            Log.d("INVITE_DEBUG", "Send Button Clicked")

            sendInvite()
        }
        binding.btnUpgrade.setOnClickListener {

            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://drivault.com".toUri()
            )

            startActivity(intent)
        }

        binding.btnExplore.setOnClickListener {

            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://drivault.com".toUri()
            )

            startActivity(intent)
        }
    }

    private fun sendInvite() {

        val binding = requireNotNull(binding)

        clearErrors(binding)

        val name =
            binding.inviteFriendsName.text?.toString()?.trim().orEmpty()

        val email =
            binding.inviteFriendsEmail.text?.toString()?.trim().orEmpty()

        val mobileUsername =
            binding.inviteFriendsMobile.text?.toString()?.trim().orEmpty()

        Log.d("INVITE_DEBUG", "Name: $name")
        Log.d("INVITE_DEBUG", "Email: $email")
        Log.d("INVITE_DEBUG", "Mobile: $mobileUsername")

        when {

            // name.isBlank() -> {
            //
            //     binding.inviteFriendsNameContainer.error =
            //         getString(R.string.invite_friends_name_required)
            //
            //     binding.inviteFriendsName.requestFocus()
            // }
            //
            // !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
            //
            //     binding.inviteFriendsEmailContainer.error =
            //         getString(R.string.invite_friends_email_invalid)
            //
            //     binding.inviteFriendsEmail.requestFocus()
            // }
            //
            // mobileUsername.isBlank() -> {
            //
            //     binding.inviteFriendsMobileContainer.error =
            //         getString(R.string.invite_friends_mobile_required)
            //
            //     binding.inviteFriendsMobile.requestFocus()
            // }
            name.isBlank() -> {

                binding.inviteFriendsNameContainer.error =
                    "Name is required"

                binding.inviteFriendsName.requestFocus()
            }

            name.length < 3 -> {

                binding.inviteFriendsNameContainer.error =
                    "Minimum 3 characters required"

                binding.inviteFriendsName.requestFocus()
            }

            !name.matches(Regex("^[a-zA-Z ]+$")) -> {

                binding.inviteFriendsNameContainer.error =
                    "Only letters allowed"

                binding.inviteFriendsName.requestFocus()
            }

            email.isBlank() -> {

                binding.inviteFriendsEmailContainer.error =
                    "Email is required"

                binding.inviteFriendsEmail.requestFocus()
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {

                binding.inviteFriendsEmailContainer.error =
                    "Invalid email address"

                binding.inviteFriendsEmail.requestFocus()
            }

            mobileUsername.isBlank() -> {

                binding.inviteFriendsMobileContainer.error =
                    "Mobile number required"

                binding.inviteFriendsMobile.requestFocus()
            }

            !mobileUsername.matches(Regex("^[0-9]+$")) -> {

                binding.inviteFriendsMobileContainer.error =
                    "Only numbers allowed"

                binding.inviteFriendsMobile.requestFocus()
            }

            mobileUsername.length != 10 -> {

                binding.inviteFriendsMobileContainer.error =
                    "Mobile number must be 10 digits"

                binding.inviteFriendsMobile.requestFocus()
            }

            else -> {
                if (isAlreadyInvited(email, mobileUsername)) {

                    DisplayUtils.showSnackMessage(
                        requireActivity(),
                        "A Drivault user with the same Email ID or Mobile Number already exists"
                    )

                    return
                }

                // Limit only 9 invites

                if (invitedFriends.size >= 9) {

                    DisplayUtils.showSnackMessage(
                        requireActivity(),
                        "You can send only 9 invitations"
                    )

                    return
                }
                //
                // binding.inviteProgress.visibility = View.VISIBLE
                // binding.inviteFriendsSend.text = ""
                // binding.inviteFriendsSend.isEnabled = false
                binding.loadingLayout.visibility = View.VISIBLE
                binding.inviteFriendsSend.text = ""
                binding.inviteFriendsSend.isClickable = false

                checkUserExists(email) { emailExists ->
                    // if (emailExists) {
                    //
                    //     binding?.loadingLayout?.visibility = View.GONE
                    //     binding?.inviteFriendsSend?.setText(R.string.write_email)
                    //
                    //     binding?.inviteFriendsSend?.isClickable = true
                    //
                    //     DisplayUtils.showSnackMessage(
                    //         requireActivity(),
                    //         "A Drivault user with the same Email ID already exists"
                    //     )
                    // }

                    // if (emailExists) {
                    //
                    //     requireActivity().runOnUiThread {
                    //
                    //         binding?.inviteProgress?.visibility =
                    //             View.GONE
                    //
                    //         binding?.inviteFriendsSend?.isEnabled =
                    //             true
                    //
                    //         DisplayUtils.showSnackMessage(
                    //             requireActivity(),
                    //             "A Drivault user with the same Email ID already exists"
                    //         )
                    //     }
                    //
                    // }

                    checkUserExists(email) { emailExists ->

                        requireActivity().runOnUiThread {

                            if (emailExists) {

                                binding?.loadingLayout?.visibility = View.GONE
                                binding?.inviteFriendsSend?.setText(R.string.write_email)
                                binding?.inviteFriendsSend?.isClickable = true

                                DisplayUtils.showSnackMessage(
                                    requireActivity(),
                                    "A Drivault user with the same Email ID already exists"
                                )

                            } else {

                                checkUserExists(mobileUsername) { mobileExists ->

                                    requireActivity().runOnUiThread {

                                        if (mobileExists) {

                                            binding?.loadingLayout?.visibility = View.GONE
                                            binding?.inviteFriendsSend?.setText(R.string.write_email)
                                            binding?.inviteFriendsSend?.isClickable = true

                                            DisplayUtils.showSnackMessage(
                                                requireActivity(),
                                                "A Drivault user with the same Mobile Number already exists"
                                            )

                                        } else {

                                            createUser(
                                                name,
                                                email,
                                                mobileUsername
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // else {
                    //
                    //     checkUserExists(
                    //         mobileUsername
                    //     ) { mobileExists ->
                    //
                    //         requireActivity().runOnUiThread {
                    //             if (mobileExists) {
                    //
                    //                 binding?.loadingLayout?.visibility = View.GONE
                    //
                    //                 binding?.inviteFriendsSend?.setText(R.string.write_email)
                    //
                    //                 binding?.inviteFriendsSend?.isClickable = true
                    //
                    //                 DisplayUtils.showSnackMessage(
                    //                     requireActivity(),
                    //                     "A Drivault user with the same Mobile Number already exists"
                    //                 )
                    //             }
                    //
                    //             // if (mobileExists) {
                    //             //
                    //             //     binding?.inviteProgress?.visibility =
                    //             //         View.GONE
                    //             //
                    //             //     binding?.inviteFriendsSend?.isEnabled =
                    //             //         true
                    //             //
                    //             //     DisplayUtils.showSnackMessage(
                    //             //         requireActivity(),
                    //             //         "A Drivault user with the same Mobile Number already exists"
                    //             //     )
                    //             //
                    //             // }
                    //             else {
                    //
                    //                 createUser(
                    //                     name,
                    //                     email,
                    //                     mobileUsername
                    //                 )
                    //             }
                    //         }
                    //     }
                    // }
                }




            }
        }
    }

    private fun clearErrors(binding: FragmentInviteFriendsBinding) {

        binding.inviteFriendsNameContainer.error = null
        binding.inviteFriendsEmailContainer.error = null
        binding.inviteFriendsMobileContainer.error = null

    }
    private fun isAlreadyInvited(
        email: String,
        mobile: String
    ): Boolean {

        return invitedFriends.any {

            it.email.equals(email, ignoreCase = true) ||
                it.mobile == mobile
        }
    }
    private fun checkUserExists(
        searchValue: String,
        onResult: (Boolean) -> Unit
    ) {

        // val client = OkHttpClient()

        val request = Request.Builder()
            .url(
                "https://login.drivault.com/ocs/v1.php/cloud/users?search=$searchValue"
            )
            .addHeader("OCS-APIRequest", "true")
            .addHeader("Accept", "application/json")
            .addHeader(
                "Authorization",
                "Basic YWRtaW46a3VSc2VmLWdvYm5vOC1nYW5rdXg="
            )
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(
                call: Call,
                e: IOException
            ) {
                Log.e("USER_CHECK", e.message ?: "")
                onResult(false)
            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {

                try {

                    val body = response.body?.string()

                    Log.d("USER_CHECK", body ?: "")

                    val json =
                        JSONObject(body ?: "")

                    val users =
                        json.getJSONObject("ocs")
                            .getJSONObject("data")
                            .getJSONArray("users")

                    onResult(users.length() > 0)

                } catch (e: Exception) {

                    Log.e(
                        "USER_CHECK",
                        "Parse Error",
                        e
                    )

                    onResult(false)
                }
            }
        })
    }

    // private fun createUser(
    //     name: String,
    //     email: String,
    //     mobileUsername: String
    //
    // ) {
    //
    //     Log.d("INVITE_DEBUG", "createUser Started")
    //     val senderEmail = "admin@gmail.com"
    //     val client = OkHttpClient()
    //
    //     val formBody = FormBody.Builder()
    //         .add("name", name)
    //         .add("email", email)
    //         .add("phone", mobileUsername)
    //         .add("inviter_email", "sender@gmail.com")
    //         .build()
    //     Log.d("INVITE_DEBUG", "Invited By: $senderEmail")
    //     Log.d("INVITE_DEBUG", "Sending API Request")
    //
    //     // Replace this IP with your PC IPv4 Address
    //     // Example: 192.168.29.10
    //
    //     val request = Request.Builder()
    //         .url("http://10.0.2.2/php_invitation_system/api/send_invite.php")
    //         .post(formBody)
    //         .build()
// }
    private fun createUser(
        name: String,
        email: String,
        mobileUsername: String
    ) {


        Log.d("INVITE_DEBUG", "createUser Started")

        // val client = OkHttpClient()

        // val senderEmail = "admin@gmail.com" // Replace with logged-in user email
        val accounts =
            AccountManager.get(requireContext()).accounts

        // val senderEmail =
        //     if (accounts.isNotEmpty()) {
        //         accounts[0].name
        //     } else {
        //         ""
        //     }
        val senderEmail =
            if (accounts.isNotEmpty()) {
                accounts[0].name.trim()
            } else {
                ""
            }

// Remove @login.drivault.com if present
        val inviterValue =
            senderEmail.replace(
                "@login.drivault.com",
                ""
            )

        Log.d(
            "INVITE_DEBUG",
            "Invited By: $inviterValue"
        )

        Log.d("INVITE_DEBUG", "Invited By: $senderEmail")

        val formBody = FormBody.Builder()
            .add("name", name)
            .add("email", email)
            .add("phone", mobileUsername)
            .add("inviter_email", inviterValue)
            // .add("inviter_email", senderEmail)
            .build()

        Log.d("INVITE_DEBUG", "Sending API Request")

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + "send_invite.php")
            // .url("http://10.0.2.2/php_invitation_system/api/send_invite.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {

                Log.e("INVITE_DEBUG", "API FAILURE: ${e.message}")

                requireActivity().runOnUiThread {
                    binding?.loadingLayout?.visibility = View.GONE
                    binding?.inviteFriendsSend?.setText(R.string.write_email)
                    // binding?.inviteFriendsSend?.isEnabled = true
                    binding?.inviteFriendsSend?.isClickable = true
                    binding?.inviteFriendsSend?.setText(R.string.write_email)
                    DisplayUtils.showSnackMessage(
                        requireActivity(),
                        "API Failed: ${e.message}"
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {

                // val responseText = response.body?.string()
                //
                // Log.d("INVITE_DEBUG", "API RESPONSE: $responseText")
                val responseText = response.body?.string()

                Log.d("INVITE_DEBUG", "API RESPONSE: $responseText")

                try {
                    val json = JSONObject(responseText ?: "")

                    Log.d(
                        "INVITE_DEBUG",
                        "Inviter Name: ${json.optString("inviter_name")}"
                    )

                    Log.d(
                        "INVITE_DEBUG",
                        "Inviter Number: ${json.optString("inviter_email")}"
                    )
                } catch (e: Exception) {
                    Log.e("INVITE_DEBUG", "JSON Parse Error", e)
                }

                requireActivity().runOnUiThread {
                    binding?.loadingLayout?.visibility = View.GONE
                    binding?.inviteFriendsSend?.setText(R.string.write_email)
                    binding?.inviteFriendsSend?.isClickable = true
                    // binding?.inviteFriendsSend?.isEnabled = true
                    binding?.inviteFriendsSend?.setText(R.string.write_email)

                    // if (response.isSuccessful) {
                    //
                    //     clearFormInputs()
                    //
                    //     fetchInvitedUsers()
                    //
                    //     DisplayUtils.showSnackMessage(
                    //         requireActivity(),
                    //         "Invite Sent Successfully"
                    //     )
                    // }
                    if (response.isSuccessful) {

                        invitedFriends.add(
                            0,
                            InviteFriend(
                                name,
                                email,
                                mobileUsername,
                                false
                            )
                        )

                        renderInvitedFriends()

                        clearFormInputs()

                        DisplayUtils.showSnackMessage(
                            requireActivity(),
                            "Invite Sent Successfully"
                        )
                    }
                    else {

                        DisplayUtils.showSnackMessage(
                            requireActivity(),
                            "Failed: $responseText"
                        )
                    }
                }
            }
        })
    }


    private fun resendInvite(
        inviteFriend: InviteFriend
    ) {

        binding?.inviteProgress?.visibility =
            View.VISIBLE

        // val client = OkHttpClient()

        val accounts =
            AccountManager.get(requireContext()).accounts

        val senderEmail =
            if (accounts.isNotEmpty()) {
                accounts[0].name
            } else {
                ""
            }

        val formBody = FormBody.Builder()
            .add("name", inviteFriend.name)
            .add("email", inviteFriend.email)
            .add("phone", inviteFriend.mobile)
            .add("inviter_email", senderEmail)
            .build()

        val request = Request.Builder()
            // .url(
            //     "http://10.0.2.2/php_invitation_system/api/get_invited_users.php?inviter_email=${senderEmail.trim()}"
            // )
            .url(
                ApiConfig.BASE_URL +
                    "get_invited_users.php?inviter_email=${senderEmail.trim()}"
            )
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(
                call: Call,
                e: IOException
            ) {

                requireActivity().runOnUiThread {

                    binding?.inviteProgress?.visibility =
                        View.GONE

                    DisplayUtils.showSnackMessage(
                        requireActivity(),
                        "Resend failed"
                    )
                }
            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {

                requireActivity().runOnUiThread {

                    binding?.inviteProgress?.visibility =
                        View.GONE

                    // Move resent invite to top
                    invitedFriends.remove(inviteFriend)

                    invitedFriends.add(0, inviteFriend)

                    saveInvitesToLocal()

                    renderInvitedFriends()

                    // Close menu
                    openedActionLayout?.let { layout ->

                        (layout.parent as? LinearLayout)
                            ?.removeView(layout)

                        openedActionLayout = null
                    }

                    DisplayUtils.showSnackMessage(
                        requireActivity(),
                        "Invitation resent successfully"
                    )
                }
            }
        })
    }
    private fun revokeInvite(
        inviteFriend: InviteFriend
    ) {

        binding?.inviteProgress?.visibility =
            View.VISIBLE

        // val client = OkHttpClient()

        val accounts =
            AccountManager
                .get(requireContext())
                .accounts

        val senderEmail =
            if (accounts.isNotEmpty())
                accounts[0].name.trim()
            else
                ""

        val inviterValue =
            senderEmail.replace(
                "@login.drivault.com",
                ""
            )

        Log.d(
            "REVOKE_DEBUG",
            "Inviter: $inviterValue"
        )

        val formBody =
            FormBody.Builder()

                .add(
                    "action",
                    "revoke"
                )

                .add(
                    "email",
                    inviteFriend.email
                )

                .add(
                    "inviter_email",
                    inviterValue
                )

                .build()

        val request =
            Request.Builder()
                .url(ApiConfig.BASE_URL + "revoke_invite.php")
                // .url(
                //     "http://10.0.2.2/php_invitation_system/api/revoke_invite.php"
                // )
                .post(formBody)
                .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    requireActivity()
                        .runOnUiThread {

                            binding?.inviteProgress
                                ?.visibility = View.GONE

                            Log.e(
                                "REVOKE_DEBUG",
                                e.message.toString()
                            )

                            DisplayUtils.showSnackMessage(
                                requireActivity(),
                                "API Failed"
                            )
                        }
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    val responseText =
                        response.body?.string()

                    Log.d(
                        "REVOKE_DEBUG",
                        responseText.toString()
                    )

                    requireActivity()
                        .runOnUiThread {

                            binding?.inviteProgress
                                ?.visibility = View.GONE

                            if (
                                response.isSuccessful
                            ) {

                                invitedFriends.remove(
                                    inviteFriend
                                )

                                saveInvitesToLocal()

                                renderInvitedFriends()

                                DisplayUtils.showSnackMessage(
                                    requireActivity(),
                                    "Invitation revoked"
                                )

                            } else {

                                DisplayUtils.showSnackMessage(
                                    requireActivity(),
                                    responseText
                                        ?: "Failed"
                                )
                            }
                        }
                }
            })
    }


    // private fun addInvite(
    //     name: String,
    //     email: String,
    //     mobileUsername: String
    // ) {
    //
    //     invitedFriends.add(
    //         InviteFriend(
    //             name = name,
    //             email = email,
    //             mobile = mobileUsername
    //         )
    //     )
    //
    //     saveInvitesToLocal()
    //
    //     renderInvitedFriends()
    //
    //     clearFormInputs()
    //
    //     DisplayUtils.showSnackMessage(
    //         requireActivity(),
    //         R.string.invite_friends_invite_sent_dummy
    //     )
    // }

    private fun addInvite(
        name: String,
        email: String,
        mobileUsername: String
    ) {
        invitedFriends.add(
            InviteFriend(
                name = name,
                email = email,
                mobile = mobileUsername
            )
        )

        saveInvitesToLocal()
        renderInvitedFriends()
        clearFormInputs()

        if (invitedFriends.size == 9) {
            showCustomSnackbar(
                getString(
                    R.string.invite_completed,
                    9
                )
            )
            // showCustomSnackbar("🎉 9 Invitations completed!")
        } else {
            DisplayUtils.showSnackMessage(
                requireActivity(),
                getString(
                    R.string.invite_sent_success
                )
            )
            // DisplayUtils.showSnackMessage(
            //     requireActivity(),
            //     "Invite Sent Successfully"
            // )
        }
    }

    private fun showCustomSnackbar(message: String) {

        val snackbar = Snackbar.make(
            requireView(),
            message,
            Snackbar.LENGTH_LONG
        )

        val snackbarView = snackbar.view

        // Get the default TextView inside Snackbar and add compound drawable
        val textView = snackbarView.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        )

        textView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.drivault_logo, // left icon
            0,
            0,
            0
        )

        textView.compoundDrawablePadding = dp(8)

        textView.setTextColor(Color.WHITE)

        textView.textSize = 15f

        // Force white tint on the drawable in case it's dark coloured
        val drawable = textView.compoundDrawables[0]

        if (drawable != null) {

            drawable.setTint(Color.WHITE)

            textView.setCompoundDrawablesWithIntrinsicBounds(
                drawable, null, null, null
            )
        }

        snackbarView.setBackgroundResource(R.drawable.toast_background)

        snackbar.show()
    }

    private fun clearFormInputs() {

        val binding = requireNotNull(binding)

        binding.inviteFriendsName.text?.clear()
        binding.inviteFriendsEmail.text?.clear()
        binding.inviteFriendsMobile.text?.clear()

        binding.inviteFriendsName.requestFocus()
    }
    private fun fetchInvitedUsers() {

        // val client = OkHttpClient()

        val accounts =
            AccountManager.get(requireContext()).accounts

        // val senderEmail =
        //     if (accounts.isNotEmpty())
        //         accounts[0].name
        //     else
        //         ""
        val senderEmail =
            if (accounts.isNotEmpty())
                accounts[0].name.trim()
            else
                ""

        val inviterValue =
            senderEmail.replace(
                "@login.drivault.com",
                ""
            )

        val request = Request.Builder()
            // .url(
            //     "http://10.0.2.2/php_invitation_system/api/get_invited_users.php?inviter_email=$senderEmail"
            // )
            // .url(
            //     "http://10.0.2.2/php_invitation_system/api/get_invited_users.php?inviter_email=$inviterValue"
            // )
            .url(
                ApiConfig.BASE_URL +
                    "get_invited_users.php?inviter_email=$inviterValue"
            )
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(
                call: Call,
                e: IOException
            ) {

                requireActivity().runOnUiThread {

                    DisplayUtils.showSnackMessage(
                        requireActivity(),
                        "Failed loading users"
                    )
                }
            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {

                val responseText =
                    response.body?.string()

                requireActivity().runOnUiThread {

                    try {

                        invitedFriends.clear()

                        val json =
                            JSONObject(responseText ?: "")

                        val users =
                            json.optJSONArray(
                                "invited_users"
                            ) ?: JSONArray()

                        for (i in 0 until users.length()) {

                            val user =
                                users.getJSONObject(i)

                            invitedFriends.add(
                                InviteFriend(
                                    name = user.optString("name"),
                                    email = user.optString("email"),
                                    mobile = user.optString("mobile_no"),
                                    isAccepted =
                                        user.optString("accepted")
                                            .equals(
                                                "yes",
                                                true
                                            )
                                )
                            )
                        }
                        invitedFriends.reverse()

                        renderInvitedFriends()

                    } catch (e: Exception) {

                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun loadInvitesFromLocal() {

        val prefs =
            requireContext().getSharedPreferences(
                "invite_storage",
                Context.MODE_PRIVATE
            )

        val savedData =
            prefs.getString(
                "invited_users",
                null
            )

        if (savedData != null) {

            invitedFriends.clear()

            val jsonArray = JSONArray(savedData)

            for (i in 0 until jsonArray.length()) {

                val obj = jsonArray.getJSONObject(i)

                invitedFriends.add(
                    InviteFriend(
                        name = obj.getString("name"),
                        email = obj.getString("email"),
                        mobile = obj.getString("mobile"),
                        isAccepted =
                            obj.getBoolean("accepted")
                    )
                )
            }

            renderInvitedFriends()

            Log.d(
                "INVITE_DEBUG",
                "Loaded Local Data"
            )
        }
    }
    private fun saveInvitesToLocal() {

        val prefs =
            requireContext().getSharedPreferences(
                "invite_storage",
                Context.MODE_PRIVATE
            )

        val jsonArray = JSONArray()

        invitedFriends.forEach { friend ->

            val obj = JSONObject()

            obj.put("name", friend.name)
            obj.put("email", friend.email)
            obj.put("mobile", friend.mobile)
            obj.put("accepted", friend.isAccepted)

            jsonArray.put(obj)
        }

        //
        // prefs.edit()
        //     .putString(
        //         "invited_users",
        //         jsonArray.toString()
        //     )
        //     .apply()


        prefs.edit {
            putString(
                "invited_users",
                jsonArray.toString()
            )
        }

        Log.d(
            "INVITE_DEBUG",
            "Saved Local Data"
        )
    }

    private fun renderInvitedFriends() {

        val binding = requireNotNull(binding)

        binding.inviteFriendsListTitle.text =
            getString(
                R.string.invited_users_count,
                invitedFriends.size
            )

        // Always show list if users exist
        binding.inviteFriendsListSection.visibility =
            if (invitedFriends.isNotEmpty())
                View.VISIBLE
            else
                View.GONE

        // Invitation limit logic
        if (invitedFriends.size >= 9) {

            binding.inviteLimitLayout.visibility =
                View.VISIBLE

            binding.inviteFriendsForm.visibility =
                View.GONE

            binding.inviteFriendsAction.visibility =
                View.GONE

            // Keep invited users visible
            binding.inviteFriendsListSection.visibility =
                View.VISIBLE

        } else {

            binding.inviteLimitLayout.visibility =
                View.GONE

            binding.inviteFriendsAction.visibility =
                View.VISIBLE
        }

        // Clear old list
        binding.inviteFriendsTable.removeAllViews()

        // Add invited users again
        invitedFriends.forEachIndexed { index, inviteFriend ->

            binding.inviteFriendsTable.addView(
                createInviteRow(index, inviteFriend)
            )
        }
    }

    private fun createInviteRow(
        index: Int,
        inviteFriend: InviteFriend
    ): View {

        val card = LinearLayout(requireContext()).apply {
            isClickable = true
            isFocusable = true
            orientation = LinearLayout.VERTICAL

            background = ContextCompat.getDrawable(
                context,
                R.drawable.invite_table_cell
            )

            setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
            )

            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            params.bottomMargin = dp(10)

            layoutParams = params
        }

        val topRow = LinearLayout(requireContext()).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL
        }

        val details = LinearLayout(requireContext()).apply {

            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val nameView = TextView(requireContext()).apply {

            text = inviteFriend.name

            textSize = 16f

            setTypeface(null, android.graphics.Typeface.BOLD)

            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.text_color
                )
            )
        }

        val emailView = TextView(requireContext()).apply {

            text = inviteFriend.email

            textSize = 13f

            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.color_accent
                )
            )
        }

        val mobileView = TextView(requireContext()).apply {

            text = inviteFriend.mobile

            textSize = 13f
        }

        details.addView(nameView)
        details.addView(emailView)
        details.addView(mobileView)

        topRow.addView(details)

        // Status Chip
        val statusView = TextView(requireContext()).apply {

            text =
                getString(
                    if (inviteFriend.isAccepted)
                        R.string.status_accepted
                    else
                        R.string.status_pending
                )
            setPadding(
                dp(12),
                dp(6),
                dp(12),
                dp(6)
            )

            setTextColor(Color.WHITE)

            textSize = 12f

            background =
                ContextCompat.getDrawable(
                    context,
                    if (inviteFriend.isAccepted)
                        R.drawable.green_chip
                    else
                        R.drawable.orange_chip
                )
        }

        topRow.addView(statusView)

        // 3 Dots Only For Pending
        if (!inviteFriend.isAccepted) {

            val moreView = TextView(requireContext()).apply {
                isClickable = true
                isFocusable = true
                text = "⋮"

                textSize = 22f

                setPadding(dp(12), 0, 0, 0)

                setOnClickListener {
// Close old opened menu
                    openedActionLayout?.let {

                        (it.parent as? LinearLayout)?.removeView(it)
                    }
                    val actionLayout =
                        LinearLayout(requireContext()).apply {

                            orientation = LinearLayout.HORIZONTAL

                            gravity = Gravity.END

                            setPadding(0, dp(12), 0, 0)
                        }

                    val resend = TextView(requireContext()).apply {

                        text = context.getString(
                            R.string.invite_action_resend
                        )

                        textSize = 14f

                        isClickable = true

                        isFocusable = true

                        setPadding(
                            dp(16),
                            dp(8),
                            dp(16),
                            dp(8)
                        )

                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.color_accent
                            )
                        )

                        setOnClickListener {


                            resendInvite(inviteFriend)

                            // Show Progress Bar
                            // binding?.inviteProgress?.visibility =
                            //     View.VISIBLE
                            //
                            // // Disable button while sending
                            // isEnabled = false

                            // Simulate resend delay
                            // postDelayed({
                            //
                            //     // Hide Progress
                            //     binding?.inviteProgress?.visibility =
                            //         View.GONE
                            //
                            //     // Enable again
                            //     isEnabled = true
                            //
                            //     // Move resent invite to TOP
                            //     invitedFriends.remove(inviteFriend)
                            //
                            //     invitedFriends.add(0, inviteFriend)
                            //
                            //     saveInvitesToLocal()
                            //
                            //     renderInvitedFriends()
                            //
                            //     // Close menu AFTER success
                            //     openedActionLayout?.let { layout ->
                            //
                            //         (layout.parent as? LinearLayout)
                            //             ?.removeView(layout)
                            //
                            //         openedActionLayout = null
                            //     }
                            //
                            //     // Show success message
                            //     DisplayUtils.showSnackMessage(
                            //         requireActivity(),
                            //         "Invitation resent successfully"
                            //     )
                            //
                            // }, 2000)
                        }
                    }

                    // val resend = TextView(requireContext()).apply {
                    //
                    //     text = context.getString(
                    //         R.string.invite_action_resend
                    //     )
                    //
                    //     setTextColor(
                    //         ContextCompat.getColor(
                    //             context,
                    //             R.color.color_accent
                    //         )
                    //     )
                    //
                    //     setPadding(dp(16), 0, dp(16), 0)
                    // }

                    val revoke = TextView(requireContext()).apply {

                        text = context.getString(
                            R.string.invite_action_revoke
                        )

                        setTextColor(Color.RED)

                        setOnClickListener {

                            revokeInvite(inviteFriend)
                        }
                    }

                    actionLayout.removeAllViews()

                    actionLayout.addView(resend)
                    actionLayout.addView(revoke)

                    if (card.childCount == 1) {
                        card.addView(actionLayout)
                        openedActionLayout = actionLayout
                    }
                }
            }

            topRow.addView(moreView)
        }

        card.addView(topRow)

        return card
    }

    //
    // private fun createInviteRow(
    //     index: Int,
    //     inviteFriend: InviteFriend
    // ): TableRow =
    //
    //     TableRow(requireContext()).apply {
    //
    //         addView(createCell((index + 1).toString(), 72))
    //         addView(createCell(inviteFriend.name, 120))
    //         addView(createEmailCell(inviteFriend))
    //         addView(createCell(inviteFriend.mobile, 120))
    //         addView(createStatusCell(inviteFriend))
    //     }

    private fun createCell(
        text: String,
        widthInDp: Int
    ): TextView =

        TextView(requireContext()).apply {

            layoutParams = TableRow.LayoutParams(
                dp(widthInDp),
                TableRow.LayoutParams.WRAP_CONTENT
            )

            background = ContextCompat.getDrawable(
                context,
                R.drawable.invite_table_cell
            )

            gravity = Gravity.CENTER_VERTICAL

            maxLines = 1

            ellipsize = TextUtils.TruncateAt.END

            setPadding(
                dp(CELL_PADDING_DP),
                dp(CELL_PADDING_DP),
                dp(CELL_PADDING_DP),
                dp(CELL_PADDING_DP)
            )

            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.text_color
                )
            )

            textSize = CELL_TEXT_SIZE_SP

            this.text = text
        }

    private fun createEmailCell(
        inviteFriend: InviteFriend
    ): TextView =

        createCell(inviteFriend.email, 140).apply {

            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.color_accent
                )
            )

            paint.isUnderlineText = true
        }

    private fun createStatusCell(
        inviteFriend: InviteFriend
    ): TextView =

        createCell(
            text = getString(
                if (inviteFriend.isAccepted) {
                    R.string.invite_friends_status_accepted
                } else {
                    R.string.invite_friends_status_pending
                }
            ),
            widthInDp = 120
        ).apply {

            if (!inviteFriend.isAccepted) {

                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.highlight_textColor_Warning
                    )
                )

                setOnClickListener {

                    invitedFriends.remove(inviteFriend)

                    renderInvitedFriends()

                    DisplayUtils.showSnackMessage(
                        requireActivity(),
                        R.string.invite_friends_invite_revoked
                    )
                }
            }
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {

        super.onSaveInstanceState(outState)

        outState.putParcelableArrayList(
            KEY_INVITED_FRIENDS,
            ArrayList(invitedFriends)
        )
    }

    override fun onDestroyView() {

        super.onDestroyView()

        binding = null
    }

    companion object {

        private const val KEY_INVITED_FRIENDS =
            "key_invited_friends"

        private const val CELL_PADDING_DP = 8

        private const val CELL_TEXT_SIZE_SP = 14f
    }
}