package `in`.sethway.ui.pair

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.github.f4b6a3.uuid.UuidCreator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import `in`.sethway.R
import `in`.sethway.databinding.DialogPickGroupNameBinding
import `in`.sethway.databinding.FragmentPairBinding

class PairFragment : Fragment() {

  private var _binding: FragmentPairBinding? = null
  private val binding get() = _binding!!

  private var _dialogBinding: DialogPickGroupNameBinding? = null
  private val dialogBinding get() = _dialogBinding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentPairBinding.inflate(layoutInflater)
    _dialogBinding = DialogPickGroupNameBinding.inflate(layoutInflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.createGroup.setOnClickListener {
      createGroupDialog()
    }
    binding.joinGroup.setOnClickListener {
      findNavController().navigate(R.id.joinFragment)
    }

    val text = "What to do?"
    ValueAnimator.ofInt(0, text.length)
      .setDuration(250)
      .also {
        it.addUpdateListener { animation ->
          binding.title.text = text.substring(0, animation.animatedValue as Int)
        }
      }.start()
  }

  private fun createGroupDialog() {
    dialogBinding.apply {
      (root.parent as ViewGroup?)?.removeView(root)

      btnUseRandomId.setOnClickListener {
        groupNameEditText.setText(UuidCreator.getTimeOrderedEpoch().toString())
      }

      var alertDialog: AlertDialog? = null
      btnContinue.setOnClickListener {
        alertDialog?.cancel()
        val args = Bundle()
        args.putString("group_id", groupNameEditText.text.toString())
        findNavController().navigate(R.id.inviteFragment, args)
      }

      alertDialog = MaterialAlertDialogBuilder(requireContext())
        .setView(root)
        .show()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
    _dialogBinding = null
  }

}