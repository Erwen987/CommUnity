package com.example.communitys.view.profile

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.communitys.databinding.FragmentProfileBinding
import com.example.communitys.view.login.LoginActivity
import com.example.communitys.viewmodel.ProfileViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Reload profile from Supabase to reflect saved changes
            viewModel.loadUserProfile()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun setupObservers() {

        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            binding.tvUserName.text = profile.name
            binding.tvFullName.text = profile.name
            binding.tvEmail.text    = profile.email
            binding.tvBarangay.text = profile.barangay
            binding.tvPoints.text   = profile.points.toString()
        }

        viewModel.logoutState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileViewModel.LogoutState.Loading -> {
                    binding.btnLogOut.isEnabled = false
                    binding.btnLogOut.text = "Logging out..."
                }
                is ProfileViewModel.LogoutState.Success -> navigateToLogin()
                is ProfileViewModel.LogoutState.Error -> {
                    binding.btnLogOut.isEnabled = true
                    binding.btnLogOut.text = "LOG OUT"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.changePasswordState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileViewModel.ActionState.Success ->
                    showSuccessToast("✅ Password changed successfully!")
                is ProfileViewModel.ActionState.Error ->
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }

        viewModel.deleteAccountState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileViewModel.ActionState.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Your account has been deleted. You may now register in a new barangay.",
                        Toast.LENGTH_LONG
                    ).show()
                    navigateToLogin()
                }
                is ProfileViewModel.ActionState.Error ->
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    // ── Click Listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.fabEditPhoto.setOnClickListener {
            val profile = viewModel.userProfile.value ?: return@setOnClickListener
            val intent = Intent(requireContext(), EditProfileActivity::class.java).apply {
                putExtra("name",     profile.name)
                putExtra("email",    profile.email)
                putExtra("barangay", profile.barangay)
            }
            editProfileLauncher.launch(intent)
        }
        binding.btnClaimReward.setOnClickListener {
            Toast.makeText(requireContext(), "Rewards feature coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnDeleteAccount.setOnClickListener  { showDeleteAccountDialog() }
        binding.btnLogOut.setOnClickListener         { showLogoutDialog() }
    }

    // ── Change Password Dialog ────────────────────────────────────────────────

    private fun showChangePasswordDialog() {
        val ctx = requireContext()

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 40, 64, 8)
        }

        // New password
        val tilNew = TextInputLayout(ctx, null,
            com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = "New password"
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 32 }
        }
        val etNew = TextInputEditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        tilNew.addView(etNew)

        // Confirm password
        val tilConfirm = TextInputLayout(ctx, null,
            com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = "Confirm new password"
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        val etConfirm = TextInputEditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        tilConfirm.addView(etConfirm)

        // Hint text
        val tvHint = TextView(ctx).apply {
            text = "🔒 Min 8 characters · letters and numbers only"
            textSize = 12f
            setTextColor(Color.parseColor("#757575"))
        }

        layout.addView(tilNew)
        layout.addView(tilConfirm)
        layout.addView(tvHint)

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Change", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Style buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#5B9BD5"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9E9E9E"))

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newPw     = etNew.text.toString()
            val confirmPw = etConfirm.text.toString()
            tilNew.error     = null
            tilConfirm.error = null

            when {
                newPw.isEmpty() ->
                { tilNew.error = "Please enter a new password"; return@setOnClickListener }
                newPw.length < 8 ->
                { tilNew.error = "Minimum 8 characters required"; return@setOnClickListener }
                !newPw.matches(Regex("^[A-Za-z0-9]+\$")) ->
                { tilNew.error = "Letters and numbers only — no symbols"; return@setOnClickListener }
                !newPw.any { it.isLetter() } ->
                { tilNew.error = "Must contain at least one letter"; return@setOnClickListener }
                !newPw.any { it.isDigit() } ->
                { tilNew.error = "Must contain at least one number"; return@setOnClickListener }
                confirmPw.isEmpty() ->
                { tilConfirm.error = "Please confirm your password"; return@setOnClickListener }
                newPw != confirmPw ->
                { tilConfirm.error = "Passwords do not match"; return@setOnClickListener }
            }

            viewModel.changePassword(newPw)
            dialog.dismiss()
        }
    }

    // ── Delete Account Dialog — Step 1: Reason ────────────────────────────────

    private fun showDeleteAccountDialog() {
        val ctx = requireContext()

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 8)
        }

        // Red warning box
        val tvWarning = TextView(ctx).apply {
            text = "⚠️  This will permanently delete your account from the barangay system.\n\nYou may register again in a different barangay after deletion."
            textSize = 13f
            setTextColor(Color.parseColor("#B71C1C"))
            setBackgroundColor(Color.parseColor("#FFEBEE"))
            setPadding(24, 20, 24, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 28 }
        }

        // Label
        val tvLabel = TextView(ctx).apply {
            text = "What is the reason for deleting your account?"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E3A5F"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }

        // Reason input
        val tilReason = TextInputLayout(ctx, null,
            com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = "e.g., Moving to another barangay"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val etReason = TextInputEditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 2
            maxLines = 4
        }
        tilReason.addView(etReason)

        layout.addView(tvWarning)
        layout.addView(tvLabel)
        layout.addView(tilReason)

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Delete Account")
            .setView(layout)
            .setPositiveButton("Next →", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#D32F2F"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9E9E9E"))

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val reason = etReason.text.toString().trim()
            tilReason.error = null

            when {
                reason.isEmpty() ->
                { tilReason.error = "Please provide a reason"; return@setOnClickListener }
                reason.length < 5 ->
                { tilReason.error = "Please provide a more detailed reason"; return@setOnClickListener }
            }

            dialog.dismiss()
            showFinalDeleteConfirmation(reason)
        }
    }

    // ── Delete Account Dialog — Step 2: Final Confirm ─────────────────────────

    private fun showFinalDeleteConfirmation(reason: String) {
        val ctx = requireContext()

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 8)
        }

        val tvMessage = TextView(ctx).apply {
            text = "Your account will be permanently deleted from the barangay system and officials will be notified of your reason."
            textSize = 14f
            setTextColor(Color.parseColor("#424242"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }

        val tvReasonLabel = TextView(ctx).apply {
            text = "Your reason:"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#757575"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 6 }
        }

        val tvReason = TextView(ctx).apply {
            text = "\"$reason\""
            textSize = 14f
            setTypeface(null, Typeface.ITALIC)
            setTextColor(Color.parseColor("#1E3A5F"))
            setBackgroundColor(Color.parseColor("#F3F6FF"))
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }

        val tvCannotUndo = TextView(ctx).apply {
            text = "⚠️ This action cannot be undone."
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#D32F2F"))
        }

        layout.addView(tvMessage)
        layout.addView(tvReasonLabel)
        layout.addView(tvReason)
        layout.addView(tvCannotUndo)

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Are you absolutely sure?")
            .setView(layout)
            .setPositiveButton("Yes, Delete My Account", null)
            .setNegativeButton("No, Keep My Account", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#D32F2F"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#5B9BD5"))

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            dialog.dismiss()
            viewModel.deleteAccount(reason)
        }
    }

    // ── Logout Dialog ─────────────────────────────────────────────────────────

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ -> viewModel.logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun showSuccessToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}