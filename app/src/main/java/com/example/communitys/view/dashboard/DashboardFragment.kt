package com.example.communitys.view.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.communitys.R
import com.example.communitys.databinding.FragmentDashboardBinding
import com.example.communitys.view.reportissue.ReportIssueActivity
import com.example.communitys.view.requestdocument.RequestDocumentActivity
import com.example.communitys.viewmodel.DashboardViewModel

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

    // ── Announcements ─────────────────────────────────────────────────────────

    private fun setupAnnouncements() {
        announcementAdapter = AnnouncementAdapter()
        binding.vpAnnouncements.adapter = announcementAdapter

        binding.vpAnnouncements.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
            }
        })
    }

    private fun buildDots(count: Int) {
        binding.llDots.removeAllViews()
        if (count <= 1) return

        val dp = resources.displayMetrics.density
        repeat(count) { i ->
            val dot = View(requireContext())
            val size  = if (i == 0) (10 * dp).toInt() else (7 * dp).toInt()
            val margin = (5 * dp).toInt()
            val lp = LinearLayout.LayoutParams(size, size).apply {
                marginStart = margin
                marginEnd   = margin
            }
            dot.layoutParams = lp
            dot.background = if (i == 0)
                ContextCompat.getDrawable(requireContext(), R.drawable.dot_active)
            else
                ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive)
            binding.llDots.addView(dot)
        }
    }

    private fun updateDots(selected: Int) {
        val dp = resources.displayMetrics.density
        for (i in 0 until binding.llDots.childCount) {
            val dot    = binding.llDots.getChildAt(i)
            val active = i == selected
            val size   = if (active) (10 * dp).toInt() else (7 * dp).toInt()
            val lp     = dot.layoutParams as LinearLayout.LayoutParams
            lp.width   = size
            lp.height  = size
            dot.layoutParams = lp
            dot.background = if (active)
                ContextCompat.getDrawable(requireContext(), R.drawable.dot_active)
            else
                ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive)
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun setupObservers() {

        viewModel.welcomeMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                binding.tvWelcome.text = message
                binding.cvHeader.visibility = View.VISIBLE
                binding.cvHeader.postDelayed({
                    _binding?.cvHeader?.animate()
                        ?.alpha(0f)?.setDuration(500)
                        ?.withEndAction {
                            _binding?.cvHeader?.visibility = View.GONE
                            _binding?.cvHeader?.alpha = 1f
                        }?.start()
                }, 5000)
            }
        }

        viewModel.locationDate.observe(viewLifecycleOwner) { binding.tvLocationDate.text = it }

        viewModel.reportsSubmitted.observe(viewLifecycleOwner) { binding.tvReportsCount.text   = it.toString() }
        viewModel.inProgress.observe(viewLifecycleOwner)       { binding.tvInProgressCount.text = it.toString() }
        viewModel.resolved.observe(viewLifecycleOwner)         { binding.tvResolvedCount.text   = it.toString() }
        viewModel.pointsEarned.observe(viewLifecycleOwner)     { binding.tvPointsCount.text     = it.toString() }

        viewModel.announcements.observe(viewLifecycleOwner) { list ->
            announcementAdapter.submitList(list)
            buildDots(list.size)

            val has = list.isNotEmpty()
            binding.vpAnnouncements.visibility       = if (has) View.VISIBLE else View.GONE
            binding.llDots.visibility                = if (has && list.size > 1) View.VISIBLE else View.GONE
            binding.layoutNoAnnouncements.visibility = if (has) View.GONE   else View.VISIBLE
            binding.tvAnnouncementCount.text         = if (has) "${list.size} post${if (list.size != 1) "s" else ""}" else ""
        }
    }

    private fun loadUserData() { viewModel.loadUserData() }

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
