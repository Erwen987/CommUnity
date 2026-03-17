package com.example.communitys.view.profile

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.load
import coil.transform.CircleCropTransformation
import com.example.communitys.R
import com.example.communitys.databinding.FragmentProfileBinding
import com.example.communitys.utils.ValidationHelper
import com.example.communitys.view.login.LoginActivity
import com.example.communitys.viewmodel.ProfileViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
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
            loadAvatar(profile.avatarUrl)
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
                    showErrorSnackbar(state.message)
                }
            }
        }

        viewModel.changePasswordState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileViewModel.ActionState.Success ->
                    showSuccessSnackbar("Password changed successfully!")
                is ProfileViewModel.ActionState.Error ->
                    showErrorSnackbar(state.message)
                else -> {}
            }
        }

        viewModel.deleteAccountState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileViewModel.ActionState.Success -> {
                    showSuccessSnackbar("Account deleted. You may register in a new barangay.")
                    navigateToLogin()
                }
                is ProfileViewModel.ActionState.Error ->
                    showErrorSnackbar(state.message)
                else -> {}
            }
        }

        viewModel.avatarUpdateState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileViewModel.ActionState.Success ->
                    showSuccessSnackbar("Avatar updated!")
                is ProfileViewModel.ActionState.Error ->
                    showErrorSnackbar(state.message)
                else -> {}
            }
        }
    }

    // ── Click Listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        // Avatar picker — tap the avatar container
        binding.flAvatarContainer.setOnClickListener { openAvatarPicker() }

        // Edit profile via FAB (original behavior, now also accessible from avatar long-press)
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

    // ── Avatar ────────────────────────────────────────────────────────────────

    private fun loadAvatar(avatarUrl: String?) {
        when {
            avatarUrl == null -> {
                // Default: show ic_profile
                binding.ivProfilePhoto.visibility  = View.VISIBLE
                binding.flPresetAvatar.visibility  = View.GONE
                binding.ivProfilePhoto.setImageResource(R.drawable.ic_profile)
            }
            avatarUrl.startsWith("preset_") -> {
                // Preset: load directly into CircleImageView so it's circular and not cropped
                val drawableRes = AvatarPickerSheet.presetDrawable(avatarUrl)
                if (drawableRes != null) {
                    binding.flPresetAvatar.visibility  = View.GONE
                    binding.ivProfilePhoto.visibility  = View.VISIBLE
                    binding.ivProfilePhoto.setImageResource(drawableRes)
                }
            }
            else -> {
                // Remote URL: load with Coil into CircleImageView
                binding.ivProfilePhoto.visibility  = View.VISIBLE
                binding.flPresetAvatar.visibility  = View.GONE
                binding.ivProfilePhoto.load(avatarUrl) {
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                    memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                    diskCachePolicy(coil.request.CachePolicy.DISABLED)
                }
            }
        }
    }

    private fun openAvatarPicker() {
        val sheet = AvatarPickerSheet.newInstance()
        sheet.listener = object : AvatarPickerSheet.AvatarSelectedListener {
            override fun onPresetSelected(presetId: String) {
                viewModel.updateAvatar(presetId)
            }
            override fun onPhotoSelected(uri: Uri) {
                uploadPhoto(uri)
            }
        }
        sheet.show(childFragmentManager, "avatar_picker")
    }

    private fun uploadPhoto(uri: Uri) {
        try {
            val bytes = requireContext().contentResolver
                .openInputStream(uri)?.readBytes() ?: return
            viewModel.uploadAndSaveAvatar(bytes)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not read photo", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Change Password Dialog ────────────────────────────────────────────────

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)

        val tilCurrent = dialogView.findViewById<TextInputLayout>(R.id.tilCurrentPassword)
        val etCurrent  = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCurrentPassword)
        val tilNew     = dialogView.findViewById<TextInputLayout>(R.id.tilNewPassword)
        val etNew      = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewPassword)
        val tilConfirm = dialogView.findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val etConfirm  = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPassword)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Change", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val currentPw = etCurrent.text.toString()
            val newPw     = etNew.text.toString()
            val confirmPw = etConfirm.text.toString()
            tilCurrent.error = null
            tilNew.error     = null
            tilConfirm.error = null

            if (currentPw.isEmpty()) {
                tilCurrent.error = "Enter your current password"
                return@setOnClickListener
            }

            with(ValidationHelper) {
                val pwResult = validatePassword(newPw)
                if (pwResult is ValidationHelper.ValidationResult.Error) {
                    tilNew.error = pwResult.message
                    return@setOnClickListener
                }
                val confirmResult = validateConfirmPassword(newPw, confirmPw)
                if (confirmResult is ValidationHelper.ValidationResult.Error) {
                    tilConfirm.error = confirmResult.message
                    return@setOnClickListener
                }
            }

            viewModel.changePassword(currentPw, newPw)
            dialog.dismiss()
        }
    }

    // ── Delete Account Dialog ─────────────────────────────────────────────────

    private fun showDeleteAccountDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete_account, null)

        val tilPassword = dialogView.findViewById<TextInputLayout>(R.id.tilPassword)
        val etPassword  = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val tilReason   = dialogView.findViewById<TextInputLayout>(R.id.tilReason)
        val etReason    = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etReason)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Next →", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val password = etPassword.text.toString()
            val reason   = etReason.text.toString().trim()
            tilPassword.error = null
            tilReason.error   = null

            when {
                password.isEmpty() ->
                { tilPassword.error = "Enter your password to confirm"; return@setOnClickListener }
                reason.isEmpty() ->
                { tilReason.error = "Please provide a reason"; return@setOnClickListener }
                reason.length < 5 ->
                { tilReason.error = "Please provide a more detailed reason"; return@setOnClickListener }
            }

            dialog.dismiss()
            showFinalDeleteConfirmation(password, reason)
        }
    }

    // ── Delete Account — Final Confirm ────────────────────────────────────────

    private fun showFinalDeleteConfirmation(password: String, reason: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete_confirm, null)

        dialogView.findViewById<android.widget.TextView>(R.id.tvReasonDisplay).text = "\"$reason\""

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Yes, Delete My Account", null)
            .setNegativeButton("No, Keep My Account", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            dialog.dismiss()
            viewModel.deleteAccount(password, reason)
        }
    }

    // ── Logout Dialog ─────────────────────────────────────────────────────────

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ -> viewModel.logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun showSuccessSnackbar(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.snackbar_success))
        snackbar.setTextColor(Color.WHITE)
        snackbar.setActionTextColor(Color.WHITE)
        val tv = snackbar.view.findViewById<android.widget.TextView>(
            com.google.android.material.R.id.snackbar_text
        )
        tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
        tv.compoundDrawablePadding = 16
        snackbar.show()
    }

    private fun showErrorSnackbar(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.snackbar_error))
        snackbar.setTextColor(Color.WHITE)
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.setActionTextColor(Color.WHITE)
        snackbar.show()
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
