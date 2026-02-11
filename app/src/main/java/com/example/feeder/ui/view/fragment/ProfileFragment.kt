package com.example.feeder.ui.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.feeder.databinding.FragmentProfileBinding
import com.example.feeder.ui.LoginActivity
import com.example.feeder.utils.PrefManager

class ProfileFragment : Fragment() {

        private val binding: FragmentProfileBinding by lazy {
            FragmentProfileBinding.inflate(layoutInflater)
        }

    private lateinit var prefManager: PrefManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefManager = PrefManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadProfileInfo()
        binding.logout.setOnClickListener {
            logoutUser()
        }
    }

    private fun loadProfileInfo() {

        binding.profileName.text = prefManager.getEmployeeName()
        binding.empId.text = prefManager.getEmployeeId()

        binding.userid.text = prefManager.getEmployeeName()
        binding.name.text = prefManager.getEmployeeId()
        binding.mob.text = prefManager.getMobileNumber()
    }


    private fun logoutUser() {

        prefManager.clear()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

}
