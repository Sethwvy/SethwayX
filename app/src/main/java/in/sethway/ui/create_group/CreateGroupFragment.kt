package `in`.sethway.ui.create_group

import android.animation.ValueAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.github.f4b6a3.uuid.UuidCreator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import `in`.sethway.R
import `in`.sethway.databinding.DialogPickNameBinding
import `in`.sethway.databinding.FragmentCreateGroupBinding

class CreateGroupFragment : Fragment() {

  private var _binding: FragmentCreateGroupBinding? = null
  private val binding get() = _binding!!

  private var _dialogBinding: DialogPickNameBinding? = null
  private val dialogBinding get() = _dialogBinding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCreateGroupBinding.inflate(inflater)
    _dialogBinding = DialogPickNameBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val text = "Create a group"
    ValueAnimator.ofInt(0, text.length)
      .setDuration(250)
      .also {
        it.addUpdateListener { animation ->
          binding.titleCreateGroup.text = text.substring(0, animation.animatedValue as Int)
        }
      }.start()

    binding.customNameButton.setOnClickListener {
      showPickNameDialog()
    }
    binding.randomIdButton.setOnClickListener {
      openGroupWith(UuidCreator.getTimeOrderedEpoch().toString())
    }
  }

  private fun showPickNameDialog() {
    (dialogBinding.root.parent as ViewGroup?)?.removeView(dialogBinding.root)
    dialogBinding.nameEditText.let {
      it.text?.clear()
      it.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(
          s: CharSequence?,
          start: Int,
          count: Int,
          after: Int
        ) {
        }

        override fun afterTextChanged(s: Editable?) {}

        override fun onTextChanged(
          s: CharSequence,
          start: Int,
          before: Int,
          count: Int
        ) {
          dialogBinding.clearNameButton.visibility = if (s.isNotEmpty()) View.VISIBLE else View.GONE
        }
      })
    }

    dialogBinding.continueButton.setOnClickListener {
      val customName = dialogBinding.nameEditText.text.toString()
      openGroupWith(customName)
    }

    MaterialAlertDialogBuilder(requireContext())
      .setView(dialogBinding.root)
      .show()
  }

  private fun openGroupWith(groupUuid: String) {
    val bundle = Bundle().apply {
      putString("group_uuid", groupUuid)
      putBoolean("create_group", true)
    }
    findNavController().navigate(R.id.myGroupFragment, bundle)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
    _dialogBinding = null
  }

}