package com.example.senseable.conversion

import android.content.Context
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
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ConversionFragment : Fragment() {

    private var imageUri: Uri? = null

    companion object {
        fun newInstance(imageUri: Uri): ConversionFragment {
            val fragment = ConversionFragment()
            fragment.arguments = Bundle().apply { putString("image_uri", imageUri.toString()) }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { imageUri = Uri.parse(it.getString("image_uri")) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_conversion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageUri?.let { processImage(it) }
            ?: run { Toast.makeText(requireContext(), "Error: No se encontró la imagen.", Toast.LENGTH_SHORT).show() }
    }

    private fun processImage(uri: Uri) {
        try {
            val inputImage = InputImage.fromFilePath(requireContext(), uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val sourceText = visionText.text
                    if (sourceText.isNotBlank()) {
                        translateTextIfNeeded(sourceText)
                    } else {
                        handleFailure("No se encontró texto en la imagen.")
                    }
                }
                .addOnFailureListener { e -> handleFailure("Error al procesar la imagen: ${e.message}") }
        } catch (e: Exception) {
            handleFailure("Error al cargar la imagen: ${e.message}")
        }
    }

    private fun translateTextIfNeeded(sourceText: String) {
        val sharedPreferences = requireActivity().getSharedPreferences("SenseAbleSettings", Context.MODE_PRIVATE)
        val targetLangTag = sharedPreferences.getString("key_language", "es-ES") ?: "es-ES"
        val targetLangCode = targetLangTag.substring(0, 2)

        LanguageIdentification.getClient().identifyLanguage(sourceText)
            .addOnSuccessListener { identifiedLangCode ->
                if (identifiedLangCode == targetLangCode) {
                    navigateToResults(sourceText, targetLangTag)
                    return@addOnSuccessListener
                }

                val sourceLang = if (targetLangCode == "es") TranslateLanguage.ENGLISH else TranslateLanguage.SPANISH

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLangCode)
                    .build()
                val translator = Translation.getClient(options)

                Toast.makeText(requireContext(), "Descargando modelo de traducción (puede tardar unos minutos la primera vez)...", Toast.LENGTH_LONG).show()

                // Permite descargar el modelo en cualquier red
                val conditions = DownloadConditions.Builder().build()

                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        translator.translate(sourceText)
                            .addOnSuccessListener { translatedText ->
                                navigateToResults(translatedText, targetLangTag)
                            }
                            .addOnFailureListener { e -> handleFailure("Error al traducir: ${e.message}", sourceText, targetLangTag) }
                    }
                    .addOnFailureListener { e -> handleFailure("No se pudo descargar el modelo de traducción. Revisa tu conexión a internet.", sourceText, targetLangTag) }
            }
            .addOnFailureListener { e -> handleFailure("No se pudo identificar el idioma del texto.", sourceText, targetLangTag) }
    }

    private fun handleFailure(message: String, fallbackText: String? = null, targetLangTag: String? = null) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        if (fallbackText != null && targetLangTag != null) {
            val resultsFragment = ResultsFragment.newInstance(fallbackText, targetLangTag)
            (activity as? MainActivity)?.navigateTo(resultsFragment)
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    private fun navigateToResults(text: String, targetLang: String) {
        val resultsFragment = ResultsFragment.newInstance(text, targetLang)
        (activity as? MainActivity)?.navigateTo(resultsFragment)
    }
}
