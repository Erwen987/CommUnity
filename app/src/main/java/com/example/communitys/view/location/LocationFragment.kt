package com.example.communitys.view.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.communitys.databinding.FragmentLocationBinding
import com.example.communitys.viewmodel.LocationViewModel

class LocationFragment : Fragment() {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LocationViewModel by viewModels()
    private lateinit var adapter: LocationReportsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LocationReportsAdapter()
        binding.recyclerReports.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerReports.adapter = adapter

        binding.cvMap.setOnClickListener {
            Toast.makeText(requireContext(), "Map interaction coming soon", Toast.LENGTH_SHORT).show()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.reports.observe(viewLifecycleOwner) { list ->
            adapter.updateList(list)
            binding.tvEmpty.visibility      = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerReports.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReports()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
