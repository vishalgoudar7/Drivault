/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.invite

import android.os.Bundle
import android.text.TextUtils
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
import javax.inject.Inject
import okhttp3.*
import java.io.IOException
import android.util.Log

class InviteFriendsFragment : Fragment() {

    private var binding: FragmentInviteFriendsBinding? = null
    private val invitedFriends = mutableListOf<InviteFriend>()

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

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
        val restoredInvites = savedInstanceState?.getParcelableArrayList<InviteFriend>(KEY_INVITED_FRIENDS).orEmpty()
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

        // binding.inviteFriendsSend.setOnClickListener {
        //     sendInvite()
        // }
        binding.inviteFriendsSend.setOnClickListener {

            Log.d("INVITE_DEBUG", "Send Button Clicked")

            sendInvite()
        }
    }

    private fun sendInvite() {
        val binding = requireNotNull(binding)
        clearErrors(binding)

        val name = binding.inviteFriendsName.text?.toString()?.trim().orEmpty()
        val email = binding.inviteFriendsEmail.text?.toString()?.trim().orEmpty()
        val mobileUsername = binding.inviteFriendsMobile.text?.toString()?.trim().orEmpty()
        Log.d("INVITE_DEBUG", "Name: $name")
        Log.d("INVITE_DEBUG", "Email: $email")
        Log.d("INVITE_DEBUG", "Mobile: $mobileUsername")

        when {
            name.isBlank() -> {
                binding.inviteFriendsNameContainer.error = getString(R.string.invite_friends_name_required)
                binding.inviteFriendsName.requestFocus()
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.inviteFriendsEmailContainer.error = getString(R.string.invite_friends_email_invalid)
                binding.inviteFriendsEmail.requestFocus()
            }

            mobileUsername.isBlank() -> {
                binding.inviteFriendsMobileContainer.error = getString(R.string.invite_friends_mobile_required)
                binding.inviteFriendsMobile.requestFocus()
            }
            else -> createUser(name, email, mobileUsername)
            // else -> addInvite(name, email, mobileUsername)
        }
    }

    private fun clearErrors(binding: FragmentInviteFriendsBinding) {
        binding.inviteFriendsNameContainer.error = null
        binding.inviteFriendsEmailContainer.error = null
        binding.inviteFriendsMobileContainer.error = null
    }


    private fun createUser(name: String, email: String, mobileUsername: String) {
        Log.d("INVITE_DEBUG", "createUser Started")
        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("displayName", name)
            .add("userid", mobileUsername)
            .add("email", email)
            .build()
        Log.d("INVITE_DEBUG", "Sending API Request")
        // val request = Request.Builder()
        //     .url("https://login.drivault.com/ocs/v1.php/cloud/users")
        //     .post(formBody)
        //     .addHeader("OCS-APIRequest", "true")
        //     .addHeader(
        //         "Authorization",
        //         Credentials.basic("admin", "kuRsef-gobno8-gankux")
        //     )
        val request = Request.Builder()
            .url("https://login.drivault.com/ocs/v1.php/cloud/users")
            .post(formBody)
            .addHeader("OCS-APIRequest", "true")
            .addHeader("Accept", "application/json")
            .addHeader(
                "Authorization",
                Credentials.basic("admin", "kuRsef-gobno8-gankux")
            )
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("INVITE_DEBUG", "API FAILURE: ${e.message}")
                requireActivity().runOnUiThread {
                    DisplayUtils.showSnackMessage(
                        requireActivity(),
                        "API Failed: ${e.message}"
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string()
                Log.d("INVITE_DEBUG", "API RESPONSE: ${response.code}")
                requireActivity().runOnUiThread {
                    if (response.isSuccessful) {

                        addInvite(name, email, mobileUsername)

                        clearFormInputs()

                        DisplayUtils.showSnackMessage(
                            requireActivity(),
                            "Invite Sent Successfully"
                        )

                    } else {

                        DisplayUtils.showSnackMessage(
                            requireActivity(),
                            "Failed : $responseText"
                        )
                    }
                    //
                    // if (response.isSuccessful) {++






















                    //
                    //     addInvite(name, email, mobileUsername)
                    //
                    //     DisplayUtils.showSnackMessage(
                    //         requireActivity(),
                    //         "Invite Sent Successfully"
                    //     )
                    //
                    // } else {
                    //
                    //     DisplayUtils.showSnackMessage(
                    //         requireActivity(),
                    //         "Failed : ${response.code}"
                    //     )
                    // }
                }
            }
        })
    }











    private fun addInvite(name: String, email: String, mobileUsername: String) {
        invitedFriends.add(
            InviteFriend(
                name = name,
                email = email,
                mobile = mobileUsername
            )
        )
        renderInvitedFriends()
        clearFormInputs()
        DisplayUtils.showSnackMessage(requireActivity(), R.string.invite_friends_invite_sent_dummy)
    }

    private fun clearFormInputs() {
        val binding = requireNotNull(binding)
        binding.inviteFriendsName.text?.clear()
        binding.inviteFriendsEmail.text?.clear()
        binding.inviteFriendsMobile.text?.clear()
        binding.inviteFriendsName.requestFocus()
    }

    private fun renderInvitedFriends() {
        val binding = requireNotNull(binding)
        binding.inviteFriendsListSection.setVisibleIf(invitedFriends.isNotEmpty())

        while (binding.inviteFriendsTable.childCount > 1) {
            binding.inviteFriendsTable.removeViewAt(1)
        }

        invitedFriends.forEachIndexed { index, inviteFriend ->
            binding.inviteFriendsTable.addView(createInviteRow(index, inviteFriend))
        }
    }

    private fun createInviteRow(index: Int, inviteFriend: InviteFriend): TableRow =
        TableRow(requireContext()).apply {
            addView(createCell((index + 1).toString(), 72))
            addView(createCell(inviteFriend.name, 120))
            addView(createEmailCell(inviteFriend))
            addView(createCell(inviteFriend.mobile, 120))
            addView(createStatusCell(inviteFriend))
        }

    private fun createCell(text: String, widthInDp: Int): TextView =
        TextView(requireContext()).apply {
            layoutParams = TableRow.LayoutParams(dp(widthInDp), TableRow.LayoutParams.WRAP_CONTENT)
            background = ContextCompat.getDrawable(context, R.drawable.invite_table_cell)
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(CELL_PADDING_DP), dp(CELL_PADDING_DP), dp(CELL_PADDING_DP), dp(CELL_PADDING_DP))
            setTextColor(ContextCompat.getColor(context, R.color.text_color))
            textSize = CELL_TEXT_SIZE_SP
            this.text = text
        }

    private fun createEmailCell(inviteFriend: InviteFriend): TextView =
        createCell(inviteFriend.email, 140).apply {
            setTextColor(ContextCompat.getColor(context, R.color.color_accent))
            paint.isUnderlineText = true
        }

    private fun createStatusCell(inviteFriend: InviteFriend): TextView =
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
                setTextColor(ContextCompat.getColor(context, R.color.highlight_textColor_Warning))
                setOnClickListener {
                    invitedFriends.remove(inviteFriend)
                    renderInvitedFriends()
                    DisplayUtils.showSnackMessage(requireActivity(), R.string.invite_friends_invite_revoked)
                }
            }
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(KEY_INVITED_FRIENDS, ArrayList(invitedFriends))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val KEY_INVITED_FRIENDS = "key_invited_friends"
        private const val CELL_PADDING_DP = 8
        private const val CELL_TEXT_SIZE_SP = 14f
    }
}
