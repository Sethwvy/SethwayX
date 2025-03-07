package `in`.sethway.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.navigation.fragment.findNavController
import `in`.sethway.App
import `in`.sethway.R
import `in`.sethway.databinding.FragmentHomeBinding
import `in`.sethway.services.sync.NotificationSyncService


class HomeFragment : Fragment() {

  private var _binding: FragmentHomeBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (App.mmkv.getBoolean("receiver", false)) {
      startSyncService()
    }
  }

  private fun startSyncService() {
    val service = Intent(requireContext(), NotificationSyncService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      requireContext().startForegroundService(service)
    } else {
      requireContext().startService(service)
    }
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
    withAnimation(binding.devicesCard) {
      findNavController().navigate(R.id.linkedDevices)
    }
    binding.addDeviceButton.setOnClickListener {
      findNavController().navigate(R.id.pairFragment)
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun withAnimation(cardView: CardView, onLift: (CardView) -> Unit) {
    cardView.setOnTouchListener { v, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          v.animate()
            .scaleX(.95f)
            .scaleY(.95f)
            .setDuration(150)
            .start()
        }

        MotionEvent.ACTION_UP -> {
          v.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(150)
            .start()
          v.performClick()
          v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
          onLift(cardView)
        }

        MotionEvent.ACTION_CANCEL -> {
          v.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(150)
            .start()
        }
      }
      true // Return true to consume the touch event
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

}