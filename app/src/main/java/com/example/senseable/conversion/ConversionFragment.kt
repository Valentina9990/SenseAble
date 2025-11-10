package com.example.senseable.conversion

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.senseable.MainActivity
import com.example.senseable.R
import com.example.senseable.results.ResultsFragment // Asegúrate de tener este import

class ConversionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_conversion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //handler para simular el proceso
        Handler(Looper.getMainLooper()).postDelayed({
            (activity as? MainActivity)?.navigateTo(ResultsFragment())
        }, 3000)
    }
}
