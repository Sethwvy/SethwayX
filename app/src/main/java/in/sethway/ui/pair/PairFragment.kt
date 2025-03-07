package `in`.sethway.ui.pair

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import `in`.sethway.App
import `in`.sethway.R
import `in`.sethway.databinding.FragmentPairBinding


class PairFragment : Fragment() {

  private var _binding: FragmentPairBinding? = null
  private val binding get() = _binding!!


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    App.setupSmartUDP()
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentPairBinding.inflate(layoutInflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.shareButton.setOnClickListener {
      findNavController().navigate(R.id.receiveFragment)
    }
    binding.receiveButton.setOnClickListener {
      findNavController().navigate(R.id.shareFragment)
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

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

}