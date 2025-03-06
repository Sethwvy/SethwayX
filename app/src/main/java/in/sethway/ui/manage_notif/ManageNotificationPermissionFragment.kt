package `in`.sethway.ui.manage_notif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.common.api.GoogleApi.Settings
import `in`.sethway.R
import `in`.sethway.adapters.ImageAdapter
import `in`.sethway.databinding.FragmentManageNotificationPermissionBinding


class ManageNotificationPermissionFragment : Fragment() {

  private var _binding: FragmentManageNotificationPermissionBinding? = null
  private val binding get() = _binding!!

  private val handler = Handler(Looper.getMainLooper())
  private lateinit var pager: ViewPager2

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentManageNotificationPermissionBinding.inflate(layoutInflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.apply {
      this@ManageNotificationPermissionFragment.pager = pager
      pager.adapter = ImageAdapter(
        requireContext(),
        arrayOf(
          R.drawable.helper_permission_1,
          R.drawable.helper_permission_2,
          R.drawable.helper_permission_3
        ).map { ContextCompat.getDrawable(requireContext(), it)!! }
      )

      circleIndicator.createIndicators(3, 0)
      circleIndicator.setViewPager(pager)

      grantPermissionButton.setOnClickListener {
        startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
      }
    }
    startAutoSlide()
  }

  private fun startAutoSlide() {
    var currentItem = 0
    val runnable = object : Runnable {
      override fun run() {
        if (currentItem == 2) currentItem = 0
        else currentItem++
        pager.currentItem = currentItem

        handler.postDelayed(this, 3500)
      }
    }
    handler.postDelayed(runnable, 3500)

    pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
      override fun onPageSelected(position: Int) {
        currentItem = position
      }
    })
  }

  override fun onResume() {
    super.onResume()
    if (canManageNotifications) {
      findNavController().navigate(R.id.homeFragment)
    }
  }

  private val canManageNotifications
    get() = android.provider.Settings.Secure.getString(
      requireContext().contentResolver,
      "enabled_notification_listeners"
    ).contains(requireContext().packageName)

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

}