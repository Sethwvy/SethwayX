package `in`.sethway.ui.group

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorShapes
import `in`.sethway.R
import `in`.sethway.databinding.FragmentInvitePeerBinding
import `in`.sethway.engine.SyncEngineService
import `in`.sethway.ui.manage_notif.ManageNotificationPermissionFragment
import inx.sethway.IGroupCallback
import inx.sethway.IIPCEngine
import kotlinx.coroutines.Runnable
import org.json.JSONObject

class InvitePeerFragment : Fragment(), ServiceConnection {

  companion object {
    private const val TAG = "MyGroupFragment"
  }

  private var _binding: FragmentInvitePeerBinding? = null
  private val binding get() = _binding!!

  private var isGroupCreator = false
  private var engineBinder: IIPCEngine? = null

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentInvitePeerBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val groupId = requireArguments().getString("group_id")
    binding.groupName.text = groupId

    binding.shareButton.setOnClickListener {
      val bitmap = ShareUtils.cardViewToBitmap(binding.qrCardView)
      ShareUtils.share(requireContext(), bitmap, "Group ID: $groupId")
    }

    val handler = Handler(Looper.getMainLooper())
    val qrCodeUpdater = object : Runnable {
      override fun run() {
        try {
          updateQRCode()
          handler.postDelayed(this, 2000)
        } catch (_: Throwable) {

        }
      }
    }
    qrCodeUpdater.run()
  }

  private fun updateQRCode() {
    val inviteInfo = engineBinder?.getInvite()
    println(inviteInfo)
    binding.apply {
      if (inviteInfo == null) {
        qrImageView.visibility = View.GONE
        qrCodeProgress.visibility = View.VISIBLE
      } else {
        qrCodeProgress.visibility = View.GONE
        qrImageView.visibility = View.VISIBLE
        qrImageView.background = createQRDrawable(inviteInfo)
      }
    }
    inviteInfo?.let { binding.qrImageView.background = createQRDrawable(it) }
  }

  override fun onStart() {
    super.onStart()
    val serviceIntent = Intent(requireContext(), SyncEngineService::class.java)
    requireContext().bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
  }

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    Log.d(TAG, "onServiceConnected")
    engineBinder = IIPCEngine.Stub.asInterface(service)

    val groupId = requireArguments().getString("group_id")!!
    engineBinder?.apply {
      isGroupCreator = true
      createGroup(groupId)
      receiveInvitee()
      registerGroupCallback(groupCallback)
    }
    updateQRCode()
  }

  private val groupCallback = object : IGroupCallback.Stub() {
    override fun onNewPeerConnected(commonInfo: String) {
      val jsonInfo = JSONObject(commonInfo)
      val displayName = jsonInfo.getString("display_name")
      Log.d(TAG, "Peer successfully added $displayName")

      requireActivity().runOnUiThread {
        Toast.makeText(
          requireContext(),
          "$displayName was added to the group", Toast.LENGTH_LONG
        ).show()
      }

      if (isGroupCreator && !ManageNotificationPermissionFragment.canManageNotifications(
          requireContext()
        )
      ) {
        findNavController().navigate(R.id.manageNotificationPermissionFragment)
      } else {
        findNavController().navigate(R.id.homeFragment)
      }
    }

    override fun onGroupJoinSuccess() {
      // This isn't for us
    }
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    Log.d(TAG, "onServiceDisconnected")
    engineBinder = null
  }

  private fun createQRDrawable(text: String): Drawable {
    return QrCodeDrawable(
      QrData.Text(text),
      QrVectorOptions.Builder()
        .setColors(
          QrVectorColors(
            dark = QrVectorColor.Solid(getAttr(com.google.android.material.R.attr.colorPrimary)),
          )
        )
        .setShapes(
          QrVectorShapes(
            darkPixel = QrVectorPixelShape.Circle(),
            ball = QrVectorBallShape.Default,
            frame = QrVectorFrameShape.RoundCorners(.2f),
          )
        )
        .setPadding(0f)
        .setErrorCorrectionLevel(QrErrorCorrectionLevel.Low)
        .build()
    )
  }

  private fun getAttr(attrId: Int): Int {
    val typedValue = TypedValue()
    requireContext().theme.resolveAttribute(attrId, typedValue, true)
    return typedValue.data
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onStop() {
    super.onStop()
    engineBinder?.unregisterGroupCallback()
    requireContext().unbindService(this)
  }

}