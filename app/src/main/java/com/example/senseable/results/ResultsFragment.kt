package com.example.senseable.results

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.senseable.R
import java.io.File
import java.io.IOException
import java.util.Locale

class ResultsFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var extractedText: String? = null
    private var isTtsInitialized = false
    private val EXPORT_AUDIO_UTTERANCE_ID = "export_audio_utterance"
    private val PLAY_AUDIO_UTTERANCE_ID = "play_audio_utterance"

    private val createTextFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.use { it.write(extractedText?.toByteArray()) }
                    Toast.makeText(requireContext(), "Archivo .txt guardado", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    Toast.makeText(requireContext(), "Error al guardar el archivo .txt", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val createAudioFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                synthesizeTextToTempFile(uri)
            }
        }
    }

    private var tempFileToCopy: File? = null
    private var finalAudioUri: Uri? = null

    companion object {
        private const val ARG_EXTRACTED_TEXT = "extracted_text"
        fun newInstance(text: String): ResultsFragment {
            val fragment = ResultsFragment(); fragment.arguments = Bundle().apply { putString(ARG_EXTRACTED_TEXT, text) }; return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(requireContext(), this)
        arguments?.let { extractedText = it.getString(ARG_EXTRACTED_TEXT) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tv_extracted_text).text = extractedText ?: "No se pudo extraer el texto."
        view.findViewById<Button>(R.id.btn_play_audio).setOnClickListener { speakOut() }
        view.findViewById<Button>(R.id.btn_export_text).setOnClickListener { exportTextToFile() }
        view.findViewById<Button>(R.id.btn_export_audio).setOnClickListener { exportAudioToFile() }
    }

    private fun exportTextToFile() {
        if (extractedText.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No hay texto para exportar.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "MiDocumento")
        }
        createTextFileLauncher.launch(intent)
    }

    private fun exportAudioToFile() {
        if (extractedText.isNullOrEmpty() || !isTtsInitialized) {
            val message = if (!isTtsInitialized) "El motor de voz no está listo." else "No hay texto para exportar."
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            return
        }
        tts.stop()
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            //mp3
            type = "audio/mpeg"
            putExtra(Intent.EXTRA_TITLE, "MiAudio")
        }
        createAudioFileLauncher.launch(intent)
    }

    private fun synthesizeTextToTempFile(destinationUri: Uri) {
        try {
            // El motor siempre genera WAV, así que el temporal debe ser .wav
            val tempFile = File.createTempFile("audio_temp", ".wav", requireContext().cacheDir)
            val params = Bundle()

            tts.synthesizeToFile(extractedText!!, params, tempFile, EXPORT_AUDIO_UTTERANCE_ID)

            tempFileToCopy = tempFile
            finalAudioUri = destinationUri
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Error al crear archivo temporal.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.forLanguageTag("es-ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "El idioma no es compatible.")
            } else {
                isTtsInitialized = true
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {
                        activity?.runOnUiThread { Toast.makeText(requireContext(), "Error al generar el audio.", Toast.LENGTH_SHORT).show() }
                    }
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == EXPORT_AUDIO_UTTERANCE_ID) {
                            copyTempFileToFinalUri()
                        }
                    }
                })
            }
        } else {
            Log.e("TTS", "Falló la inicialización de TTS.")
        }
    }

    private fun copyTempFileToFinalUri() {
        try {
            tempFileToCopy?.inputStream()?.use { input ->
                requireContext().contentResolver.openOutputStream(finalAudioUri!!)?.use { output ->
                    input.copyTo(output)
                }
            }
            activity?.runOnUiThread { Toast.makeText(requireContext(), "Archivo de audio guardado.", Toast.LENGTH_SHORT).show() }
        } catch (e: IOException) {
            activity?.runOnUiThread { Toast.makeText(requireContext(), "Error al guardar el archivo de audio.", Toast.LENGTH_SHORT).show() }
        } finally {
            tempFileToCopy?.delete()
        }
    }

    private fun speakOut() {
        if (!isTtsInitialized) return

        tts.stop()

        if (!extractedText.isNullOrEmpty()) {
            tts.speak(extractedText, TextToSpeech.QUEUE_FLUSH, null, PLAY_AUDIO_UTTERANCE_ID)
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
