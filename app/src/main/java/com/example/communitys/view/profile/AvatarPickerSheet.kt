package com.example.communitys.view.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.communitys.R
import com.example.communitys.databinding.BottomSheetAvatarPickerBinding
import com.example.communitys.databinding.ItemAvatarPresetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AvatarPickerSheet : BottomSheetDialogFragment() {

    interface AvatarSelectedListener {
        fun onPresetSelected(presetId: String)
        fun onPhotoSelected(uri: Uri)
    }

    var listener: AvatarSelectedListener? = null

    private var _binding: BottomSheetAvatarPickerBinding? = null
    private val binding get() = _binding!!

    // Preset: id → background drawable res
    private val presets = listOf(
        "preset_1" to R.drawable.avatar_bg_1,
        "preset_2" to R.drawable.avatar_bg_2,
        "preset_3" to R.drawable.avatar_bg_3,
        "preset_4" to R.drawable.avatar_bg_4,
        "preset_5" to R.drawable.avatar_bg_5,
        "preset_6" to R.drawable.avatar_bg_6
    )

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            listener?.onPhotoSelected(uri)
            dismiss()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) imagePickerLauncher.launch("image/*")
        else Toast.makeText(requireContext(), "Permission required to pick a photo", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAvatarPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAvatarPresets.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvAvatarPresets.adapter = PresetAdapter(presets) { presetId ->
            listener?.onPresetSelected(presetId)
            dismiss()
        }

        binding.btnUploadPhoto.setOnClickListener { requestGalleryPermission() }
    }

    private fun requestGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED ->
                imagePickerLauncher.launch("image/*")
            else -> permissionLauncher.launch(permission)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Preset Adapter ────────────────────────────────────────────────────────

    private class PresetAdapter(
        private val items: List<Pair<String, Int>>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<PresetAdapter.VH>() {

        private var selectedId: String? = null

        inner class VH(val binding: ItemAvatarPresetBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(ItemAvatarPresetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (id, bgRes) = items[position]
            val isSelected = id == selectedId
            holder.binding.ivAvatarCircle.setImageResource(bgRes)
            holder.binding.ivSelectedRing.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.binding.tvCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                selectedId = id
                notifyDataSetChanged()
                onClick(id)
            }
        }

        override fun getItemCount() = items.size
    }

    companion object {
        fun newInstance() = AvatarPickerSheet()

        // Map preset ID → drawable resource (used when loading the saved avatar)
        fun presetDrawable(presetId: String): Int? = when (presetId) {
            "preset_1" -> R.drawable.avatar_bg_1
            "preset_2" -> R.drawable.avatar_bg_2
            "preset_3" -> R.drawable.avatar_bg_3
            "preset_4" -> R.drawable.avatar_bg_4
            "preset_5" -> R.drawable.avatar_bg_5
            "preset_6" -> R.drawable.avatar_bg_6
            else -> null
        }
    }
}
