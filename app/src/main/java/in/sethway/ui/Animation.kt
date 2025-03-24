package `in`.sethway.ui

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.cardview.widget.CardView

object Animation {
  fun withCardAnimation(cardView: CardView, onLift: (CardView) -> Unit) {
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
}