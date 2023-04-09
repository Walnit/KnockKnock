package com.example.knockknock.onboarding

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.ValueAnimator
import android.animation.ValueAnimator.REVERSE
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.knockknock.R

class Page3 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_ob_3_knocking, container, false)

        val ob2 = view.findViewById<ConstraintLayout>(R.id.ob_3)
        val ob2Btn = view.findViewById<Button>(R.id.ob_3_btn)
        val fist = view.findViewById<ImageView>(R.id.ob_3_fist)

        AnimatorInflater.loadAnimator(requireContext(), R.animator.messages_attach_in).apply {
            setTarget(ob2)
            start()
        }

        ValueAnimator.ofFloat(-20.0f, 20.0f).apply {
            addUpdateListener {
                fist.rotationX = it.animatedValue as Float
            }
            interpolator = AccelerateInterpolator(2.0f)
            repeatMode = REVERSE
            repeatCount = -1
            start()
        }

        ob2Btn.setOnClickListener {
            AnimatorInflater.loadAnimator(requireContext(), R.animator.messages_attach_out).apply {
                setTarget(ob2)
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        with (parentFragmentManager.beginTransaction()) {
                            replace(R.id.ob_frag_container_view, Page4())
                            addToBackStack(null)
                            commit()
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                    }

                    override fun onAnimationRepeat(animation: Animator) {
                    }
                })
                start()
            }

        }
        return view
    }

}