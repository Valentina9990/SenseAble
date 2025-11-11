package com.example.senseable.conversion

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.senseable.MainActivity
import com.example.senseable.R
import com.example.senseable.results.ResultsFragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ConversionFragment : Fragment() {

    private var imageUri: Uri? = null

    companion object {
        private const val ARG_IMAGE_URI = "image_uri"

        fun newInstance(imageUri: Uri): ConversionFragment {
            val fragment = ConversionFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE_URI, imageUri.toString())
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUri = Uri.parse(it.getString(ARG_IMAGE_URI))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_conversion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageUri?.let {
            processImage(it)
        } ?: run {
            Toast.makeText(requireContext(), "Error: No se encontró la imagen.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImage(uri: Uri) {
        try {
            val inputImage = InputImage.fromFilePath(requireContext(), uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val extractedText = visionText.text
                    navigateToResults(extractedText)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error al procesar la imagen: ${e.message}", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al cargar la imagen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToResults(text: String) {
        if (text.isNotBlank()) {
            // Si hay texto, navegamos y lo pasamos.
            val resultsFragment = ResultsFragment.newInstance(text)
            (activity as? MainActivity)?.navigateTo(resultsFragment)
        } else {
            // Si ML Kit no encontró texto, mostramos un error y volvemos a la pantalla de bienvenida.
            Toast.makeText(requireContext(), "No se encontró texto en la imagen seleccionada.", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }
    }
}
