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

    private val colorActive   = Color.parseColor("#2C5F7F")
    private val colorInactive = Color.parseColor("#D3D3D3")
    private val textActive    = Color.WHITE
    private val textInactive  = Color.parseColor("#7A7A7A")

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
        adapter = DocumentsAdapter(onViewDetails = { item ->
            DocumentDetailSheet.newInstance(
                title           = item.title,
                reference       = item.reference,
                status          = item.status,
                date            = item.date,
                purpose         = item.purpose,
                paymentMethod   = item.paymentMethod,
                proofUrl        = item.proofUrl,
                rejectionReason = item.rejectionReason
            ).show(childFragmentManager, "doc_detail")
        })
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupTabs() {
        binding.btnMyRequest.setOnClickListener { setActiveTab("requests") }
        binding.btnHistory.setOnClickListener   { setActiveTab("history") }
    }

    private var currentTab = "requests"

    private fun setActiveTab(tab: String) {
        currentTab = tab
        viewModel.setTab(tab)

        listOf(binding.btnMyRequest, binding.btnHistory).forEach { btn ->
            btn.backgroundTintList = ColorStateList.valueOf(colorInactive)
            btn.setTextColor(textInactive)
        }

        val activeBtn = if (tab == "history") binding.btnHistory else binding.btnMyRequest
        activeBtn.backgroundTintList = ColorStateList.valueOf(colorActive)
        activeBtn.setTextColor(textActive)

        binding.chipScrollView.visibility = if (tab == "requests") View.VISIBLE else View.GONE
    }

    private fun setupChips() {
        // Set initial visual state
        updateChipStyles(binding.chipAll.id)

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: binding.chipAll.id
            updateChipStyles(checkedId)

            val filter = when (checkedId) {
                binding.chipPending.id        -> "pending"
                binding.chipReadyForPickup.id -> "ready_for_pickup"
                else                          -> "all"
            }
            viewModel.setFilter(filter)
        }
    }

    private fun updateChipStyles(activeId: Int) {
        val chips = listOf(binding.chipAll, binding.chipPending, binding.chipReadyForPickup)
        chips.forEach { chip ->
            if (chip.id == activeId) {
                chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#2C5F7F"))
                chip.setTextColor(Color.WHITE)
            } else {
                chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
                chip.setTextColor(Color.parseColor("#7A7A7A"))
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.items.observe(viewLifecycleOwner) { list ->
            adapter.updateList(list)
            if (list.isEmpty()) {
                binding.tvEmpty.visibility      = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.tvEmpty.text = if (currentTab == "history")
                    "No completed or rejected requests yet."
                else
                    "No active requests.\nTap 'Request Document' on the dashboard."
            } else {
                binding.tvEmpty.visibility      = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
