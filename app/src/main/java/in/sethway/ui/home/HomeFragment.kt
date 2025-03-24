package `in`.sethway.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.sethway.R
import `in`.sethway.databinding.FragmentHomeBinding
import `in`.sethway.ui.Animation.withCardAnimation

//import `in`.sethway.engine.group_old.Group


class HomeFragment : Fragment() {

  private var _binding: FragmentHomeBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentHomeBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    withCardAnimation(binding.devicesCard) {
      findNavController().navigate(R.id.manageGroupFragment)
    }
  }

  @SuppressLint("ClickableViewAccessibility")

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

}