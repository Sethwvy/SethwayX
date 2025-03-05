package `in`.sethway.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.sethway.R
import `in`.sethway.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {


  private var _binding: FragmentWelcomeBinding? = null
  private val binding get() = _binding!!


  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentWelcomeBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.shareButton.setOnClickListener {
      findNavController().navigate(R.id.broadcastFragment)
    }
    binding.receiveButton.setOnClickListener {
      findNavController().navigate(R.id.connectFragment)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

}