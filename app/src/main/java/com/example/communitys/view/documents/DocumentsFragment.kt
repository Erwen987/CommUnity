package com.example.communitys.view.documents

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.communitys.databinding.FragmentDocumentsBinding
import com.example.communitys.viewmodel.DocumentsViewModel

class DocumentsFragment : Fragment() {

    private var _binding: FragmentDocumentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DocumentsViewModel by viewModels()
    private lateinit var adapter: DocumentsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDocumentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTabs()
        setupChips()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = DocumentsAdapter(onViewDetails = { request ->
            Toast.makeText(
                requireContext(),
                "${request.documentType}\n${request.referenceNumber} — ${request.status.replace('_', ' ')}",
                Toast.LENGTH_LONG
            ).show()
        })
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupTabs() {
        binding.btnMyRequest.setOnClickListener { setActiveTab("active") }
        binding.btnHistory.setOnClickListener { setActiveTab("history") }
    }

    private fun setActiveTab(tab: String) {
        viewModel.setTab(tab)
        if (tab == "active") {
            binding.btnMyRequest.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2C5F7F"))
            binding.btnMyRequest.setTextColor(Color.WHITE)
            binding.btnHistory.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            binding.btnHistory.setTextColor(Color.parseColor("#7A7A7A"))
            binding.chipScrollView.visibility = View.VISIBLE
        } else {
            binding.btnHistory.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2C5F7F"))
            binding.btnHistory.setTextColor(Color.WHITE)
            binding.btnMyRequest.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            binding.btnMyRequest.setTextColor(Color.parseColor("#7A7A7A"))
            binding.chipScrollView.visibility = View.GONE
        }
    }

    private fun setupChips() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                checkedIds.contains(binding.chipReviewing.id)      -> "reviewing"
                checkedIds.contains(binding.chipProcessing.id)     -> "processing"
                checkedIds.contains(binding.chipReadyForPickup.id) -> "ready_for_pickup"
                checkedIds.contains(binding.chipReleased.id)       -> "released"
                else                                               -> "all"
            }
            viewModel.setFilter(filter)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.requests.observe(viewLifecycleOwner) { list ->
            adapter.updateList(list)
            binding.tvEmpty.visibility    = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadRequests() // Refresh when returning from RequestDocumentActivity
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
