package `in`.sethway.ui.group

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import `in`.sethway.databinding.FragmentMyGroupBinding
import `in`.sethway.engine.group_old.Group
import `in`.sethway.engine.group_old.SimpleGroupSync
import `in`.sethway.ui.manage_notif.ManageNotificationPermissionFragment


class MyGroupFragment : Fragment() {

  companion object {
    private const val TAG = "GroupFragment"
  }

  private var _binding: FragmentMyGroupBinding? = null
  private val binding get() = _binding!!

  private lateinit var groupSync: SimpleGroupSync

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    groupSync = SimpleGroupSync(
      weJoined = { },
      someoneJoined = { peerInfo ->
        val deviceName = peerInfo.getString("device_name")

        // Now ask them if they wanna continue or add another one
        Toast.makeText(
          requireContext(),
          "$deviceName was added to the group!",
          Toast.LENGTH_LONG
        ).show()

        if (Group.isGroupCreator()
          && !ManageNotificationPermissionFragment.canManageNotifications(requireContext())
        ) {
          findNavController().navigate(R.id.manageNotificationPermissionFragment)
        } else {
          findNavController().navigate(R.id.homeFragment)
        }
      }
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentMyGroupBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val args = requireArguments()

    val groupUUID = args.getString("group_uuid") ?: "Teddybär-Gruppe" // for now
    val createNewGroup = args.getBoolean("create_group")

    binding.groupName.text = groupUUID

    if (createNewGroup) {
      Group.createGroup(groupUUID)
    }

    val handler = Handler(Looper.getMainLooper())
    val qrUpdater = object : Runnable {
      override fun run() {
        try {
          val inviteInfo = groupSync.getInviteInfo().toString()
          println(inviteInfo)
          binding.qrImageView.background = createQRDrawable(inviteInfo)
          handler.postDelayed(this, 5000)
        } catch (_: Throwable) {

        }
      }
    }
    handler.post(qrUpdater)

    binding.shareButton.setOnClickListener {
      val bitmap = ShareUtils.cardViewToBitmap(binding.qrCardView)
      ShareUtils.share(requireContext(), bitmap, "Group name: $groupUUID")
    }
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

  override fun onDestroy() {
    super.onDestroy()
    groupSync.close()
  }

}