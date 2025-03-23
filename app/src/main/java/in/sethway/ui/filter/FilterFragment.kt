package `in`.sethway.ui.filter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import `in`.sethway.App.Companion.GROUP
import `in`.sethway.R
import `in`.sethway.databinding.FragmentFilterBinding
import `in`.sethway.ui.adapters.AppAdapter
import `in`.sethway.ui.adapters.AppInfo
import `in`.sethway.ui.manage_notif.ManageNotificationPermissionFragment
import io.paperdb.Paper
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.json.JSONArray
import org.json.JSONObject


class FilterFragment : Fragment() {

  companion object {
    private val DUMMY_PEER_INFO = JSONObject()
      .put("display_name", "Nothing Phone (2a)")
      .put("id", "hello-world")
      .toString()
  }

  private var _binding: FragmentFilterBinding? = null
  private val binding get() = _binding!!

  private lateinit var peerName: String
  private lateinit var peerId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val jsonInfo = JSONObject(arguments?.getString("peer_info") ?: DUMMY_PEER_INFO)
    peerName = jsonInfo.getString("display_name")
    peerId = jsonInfo.getString("id")
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentFilterBinding.inflate(layoutInflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.descriptionFilter.text = getString(R.string.description_filter, peerName)

    val appsList = getNotificationApps()

    val checkedApps = mutableListOf<AppInfo>()
    val appAdapter = AppAdapter(appsList) { appInfo, checked ->
      if (checked) {
        checkedApps += appInfo
      } else {
        checkedApps -= appInfo
      }
      binding.pickedApps.text = checkedApps.joinToString(", ") { it.appName }
    }

    val layoutManager = LinearLayoutManager(requireContext())
    binding.apps.apply {
      this.layoutManager = layoutManager
      adapter = appAdapter
      addItemDecoration(
        DividerItemDecoration(
          requireContext(),
          layoutManager.orientation
        )
      )
      FastScrollerBuilder(this).useMd2Style().build()
    }

    binding.continueButton.setOnClickListener {
      writePeerAppList(checkedApps)

      val needsManageNotificationPermission =
        GROUP.amCreator && !ManageNotificationPermissionFragment.canManageNotifications(
          requireContext()
        )

      if (needsManageNotificationPermission) {
        findNavController().navigate(R.id.manageNotificationPermissionFragment)
      } else {
        findNavController().navigate(R.id.homeFragment)
      }
    }
  }

  private fun writePeerAppList(checkedApps: MutableList<AppInfo>) {
    val packageNames = checkedApps.map { it.packageName }
    val book = Paper.book("peer_app_list")
    book.write(peerId, JSONArray(packageNames).toString())
  }

  private fun getNotificationApps(): MutableList<AppInfo> {
    val apps = mutableListOf<AppInfo>()

    val packageManager = requireContext().packageManager
    val mainIntent =
      Intent(Intent.ACTION_MAIN, null).also { it.addCategory(Intent.CATEGORY_LAUNCHER) }

    val resolvedInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      packageManager.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
      packageManager.queryIntentActivities(mainIntent, 0)
    }

    for (resolveInfo in resolvedInfos) {
      val appName = resolveInfo.loadLabel(packageManager).toString()
      val appIcon = resolveInfo.loadIcon(packageManager)
      val packageName = resolveInfo.activityInfo.packageName

      if (canPostNotifications(packageName)) {
        apps.add(AppInfo(appName, appIcon, packageName))
      }
    }
    apps.sortBy { it.appName }
    return apps
  }

  private fun canPostNotifications(packageName: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    val packageInfo =
      requireContext().packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)

    if ((packageInfo.applicationInfo?.targetSdkVersion ?: 0) < Build.VERSION_CODES.TIRAMISU)
      return true

    return packageInfo.requestedPermissions?.contains(Manifest.permission.POST_NOTIFICATIONS) == true
  }


  private fun getAssetBitmap(fileName: String): Bitmap =
    requireContext().assets.open(fileName).use { BitmapFactory.decodeStream(it) }


  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}