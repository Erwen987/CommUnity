package com.example.communitys.view.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.communitys.databinding.FragmentDashboardBinding
import com.example.communitys.view.reportissue.ReportIssueActivity
import com.example.communitys.view.requestdocument.RequestDocumentActivity
import com.example.communitys.viewmodel.DashboardViewModel
import com.google.android.material.tabs.TabLayoutMediator

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel
    private lateinit var announcementAdapter: AnnouncementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(DashboardViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAnnouncements()
        setupObservers()
        setupClickListeners()
        loadUserData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStats()
    }

    private fun setupAnnouncements() {
        announcementAdapter = AnnouncementAdapter()
        binding.vpAnnouncements.adapter = announcementAdapter

        TabLayoutMediator(binding.tlDots, binding.vpAnnouncements) { _, _ -> }.attach()
    }

    private fun setupObservers() {

        // Welcome card
        viewModel.welcomeMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                binding.tvWelcome.text = message
                binding.cvHeader.visibility = View.VISIBLE

                binding.cvHeader.postDelayed({
                    _binding?.cvHeader?.animate()
                        ?.alpha(0f)
                        ?.setDuration(500)
                        ?.withEndAction {
                            _binding?.cvHeader?.visibility = View.GONE
                            _binding?.cvHeader?.alpha = 1f
                        }
                        ?.start()
                }, 5000)
            }
        }

        viewModel.locationDate.observe(viewLifecycleOwner) { locationDate ->
            binding.tvLocationDate.text = locationDate
        }

        viewModel.reportsSubmitted.observe(viewLifecycleOwner) { count ->
            binding.tvReportsCount.text = count.toString()
        }

        viewModel.inProgress.observe(viewLifecycleOwner) { count ->
            binding.tvInProgressCount.text = count.toString()
        }

        viewModel.resolved.observe(viewLifecycleOwner) { count ->
            binding.tvResolvedCount.text = count.toString()
        }

        viewModel.pointsEarned.observe(viewLifecycleOwner) { points ->
            binding.tvPointsCount.text = points.toString()
        }

        // Announcements
        viewModel.announcements.observe(viewLifecycleOwner) { list ->
            announcementAdapter.submitList(list)

            val hasAnnouncements = list.isNotEmpty()
            binding.vpAnnouncements.visibility       = if (hasAnnouncements) View.VISIBLE else View.GONE
            binding.tlDots.visibility                = if (hasAnnouncements) View.VISIBLE else View.GONE
            binding.layoutNoAnnouncements.visibility = if (hasAnnouncements) View.GONE   else View.VISIBLE
            binding.tvAnnouncementCount.text         = if (hasAnnouncements) "${list.size} post${if (list.size != 1) "s" else ""}" else ""
        }
    }

    private fun loadUserData() {
        viewModel.loadUserData()
    }

    private fun setupClickListeners() {
        binding.ivNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnReportIssue.setOnClickListener {
            startActivity(Intent(requireContext(), ReportIssueActivity::class.java))
        }

        binding.btnRequestDocument.setOnClickListener {
            startActivity(Intent(requireContext(), RequestDocumentActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
