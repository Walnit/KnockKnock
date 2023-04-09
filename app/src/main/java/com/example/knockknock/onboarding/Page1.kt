package com.example.knockknock.onboarding

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.knockknock.R

class Page1 : Fragment() {
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.onboarding_pg1_intro, container, false)

        val ob1 = view.findViewById<View>(R.id.ob_1_1)
        val ob2 = view.findViewById<View>(R.id.ob_1_2)
        val ob3 = view.findViewById<View>(R.id.ob_1_3)
        val ob4 = view.findViewById<View>(R.id.ob_1_4)
        val ob5 = view.findViewById<View>(R.id.ob_1_5)

        val fadeIn = AnimatorInflater.loadAnimator(requireContext(), R.animator.messages_attach_in)
        fadeIn.setTarget(ob1)
        fadeIn.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                AnimatorInflater.loadAnimator(requireContext(), R.animator.messages_attach_in).apply {
                    setTarget(ob2)
                    addListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            AnimatorInflater.loadAnimator(requireContext(), R.animator.messages_attach_in).apply {
                                setTarget(ob3)
                                addListener(object : Animator.AnimatorListener {
                                    override fun onAnimationStart(animation: Animator) {
                                    }

                                    override fun onAnimationEnd(animation: Animator) {
                                        AnimatorInflater.loadAnimator(requireContext(), R.animator.messages_attach_in).apply {
                                            setTarget(ob4)
                                            addListener(object : Animator.AnimatorListener {
                                                override fun onAnimationStart(animation: Animator) {
                                                }

                                                override fun onAnimationEnd(animation: Animator) {
                                                    AnimatorInflater.loadAnimator(requireContext(), R.animator.messages_attach_in).apply {
                                                        setTarget(ob5)
                                                        start()
                                                    }
                                                }

                                                override fun onAnimationCancel(animation: Animator) {
                                                }

                                                override fun onAnimationRepeat(animation: Animator) {
                                                }
                                            })
                                            startDelay = 1000
                                            start()
                                        }
                                    }

                                    override fun onAnimationCancel(animation: Animator) {
                                    }

                                    override fun onAnimationRepeat(animation: Animator) {
                                    }
                                })
                                startDelay = 500
                                start()
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

            override fun onAnimationCancel(animation: Animator) {
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        })
        fadeIn.start()

        (ob5 as Button).setOnClickListener {
            AnimatorInflater.loadAnimator(requireContext(), R.animator.messages_attach_out).apply {
                setTarget(view.findViewById<ConstraintLayout>(R.id.ob_1))
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        with (parentFragmentManager.beginTransaction()) {
                            replace(R.id.ob_frag_container_view, Page2())
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
