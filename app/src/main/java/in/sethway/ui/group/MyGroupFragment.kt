package `in`.sethway.ui.group

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
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
import `in`.sethway.databinding.FragmentMyGroupBinding
import `in`.sethway.engine.GroupManagement
import `in`.sethway.ui.share.ShareUtils


class MyGroupFragment : Fragment() {

  companion object {
    private const val TAG = "GroupFragment"
  }

  private var _binding: FragmentMyGroupBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    GroupManagement.open { groupCreated: Boolean ->
      Log.d(TAG, "Group created successfully!")
    }
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
      GroupManagement.createGroup(groupUUID)
    }

    val handler = Handler(Looper.getMainLooper())
    val qrUpdater = object : Runnable {
      override fun run() {
        try {
          binding.qrImageView.background = createQRDrawable(GroupManagement.getGroupInfo())
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

  override fun onDestroy() {
    super.onDestroy()
    _binding = null
    GroupManagement.close()
  }

}