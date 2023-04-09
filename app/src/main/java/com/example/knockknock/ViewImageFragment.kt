package com.example.knockknock

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.transition.TransitionInflater

class ViewImageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.shared_image)
        // Inflate the layout for this fragment
        val imgData: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable("imgData", Bitmap::class.java)
        } else {
            requireArguments().getParcelable("imgData")
        }
        val view = inflater.inflate(R.layout.fragment_view_image, container, false)
        view.findViewById<ImageView>(R.id.imageviewer_image).setImageBitmap(imgData)
        return view
    }

}