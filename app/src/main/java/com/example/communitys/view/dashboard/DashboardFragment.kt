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

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
        loadUserData()
    }

    // Refresh stats every time user comes back to this tab
    override fun onResume() {
        super.onResume()
        viewModel.refreshStats()
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

        // ✅ Real report stats
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
    }

    private fun loadUserData() {
        val prefs = requireActivity().getSharedPreferences("CommUnityPrefs", 0)

        // Check if user has ever logged in before
        val hasLoggedInBefore = prefs.getBoolean("hasLoggedInBefore", false)

        // Pass this to ViewModel
        viewModel.loadUserData(hasLoggedInBefore)

        // Mark that user has now logged in at least once
        if (!hasLoggedInBefore) {
            prefs.edit().putBoolean("hasLoggedInBefore", true).apply()
        }
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