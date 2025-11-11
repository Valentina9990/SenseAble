package com.example.senseable.results

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
    private lateinit var sharedPreferences: SharedPreferences
    private var processedText: String? = null
    private var targetLanguageTag: String? = null
    private var isTtsInitialized = false

    private val EXPORT_AUDIO_UTTERANCE_ID = "export_audio_utterance"
    private val PLAY_AUDIO_UTTERANCE_ID = "play_audio_utterance"

    private val createTextFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.use { it.write(processedText?.toByteArray()) }
                    Toast.makeText(requireContext(), "Archivo .txt guardado", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) { Toast.makeText(requireContext(), "Error al guardar el archivo .txt", Toast.LENGTH_SHORT).show() }
            }
        }
    }
    private val createAudioFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> synthesizeTextToTempFile(uri) }
        }
    }
    private var tempFileToCopy: File? = null
    private var finalAudioUri: Uri? = null

    companion object {
        private const val ARG_TEXT = "arg_text"
        private const val ARG_LANG = "arg_lang"
        fun newInstance(text: String, targetLang: String): ResultsFragment {
            val fragment = ResultsFragment(); fragment.arguments = Bundle().apply {
                putString(ARG_TEXT, text)
                putString(ARG_LANG, targetLang)
            }; return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(requireContext(), this)
        sharedPreferences = requireActivity().getSharedPreferences("SenseAbleSettings", Context.MODE_PRIVATE)
        arguments?.let {
            processedText = it.getString(ARG_TEXT)
            targetLanguageTag = it.getString(ARG_LANG)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_results, container, false)
    }

    private fun speakOut() {
        if (processedText.isNullOrEmpty() || !isTtsInitialized) return
        applySettingsToTts()
        tts.stop()
        tts.speak(processedText, TextToSpeech.QUEUE_FLUSH, null, PLAY_AUDIO_UTTERANCE_ID)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tv_extracted_text).text = processedText ?: "No se pudo procesar el texto."
        view.findViewById<Button>(R.id.btn_play_audio).setOnClickListener { speakOut() }
        view.findViewById<Button>(R.id.btn_export_text).setOnClickListener { exportTextToFile() }
        view.findViewById<Button>(R.id.btn_export_audio).setOnClickListener { exportAudioToFile() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onError(utteranceId: String?) { activity?.runOnUiThread { Toast.makeText(requireContext(), "Error al generar el audio.", Toast.LENGTH_SHORT).show() } }
                override fun onDone(utteranceId: String?) { if (utteranceId == EXPORT_AUDIO_UTTERANCE_ID) copyTempFileToFinalUri() }
            })
        } else {
            Log.e("TTS", "Falló la inicialización de TTS.")
        }
    }

    private fun applySettingsToTts() {
        if (!isTtsInitialized) return

        val locale = Locale.forLanguageTag(targetLanguageTag ?: "es-ES")
        val langResult = tts.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "Idioma no compatible: $targetLanguageTag")
            Toast.makeText(requireContext(), "Idioma no compatible: $targetLanguageTag", Toast.LENGTH_SHORT).show()
        }

        val speed = sharedPreferences.getFloat("key_speed", 1.0f)
        tts.setSpeechRate(speed)

        val voiceGender = sharedPreferences.getString("key_voice", "FEMALE")
        val localeLanguage = locale.language

        val voices = tts.voices

        voices?.let {
            var foundVoice = it.find { voice ->
                val langMatches = voice.locale.language == localeLanguage
                val genderMatches = if (voiceGender == "MALE")
                    voice.name.contains("male", ignoreCase = true)
                else
                    voice.name.contains("female", ignoreCase = true)
                langMatches && genderMatches
            }

            if (foundVoice == null) {
                foundVoice = it.find { voice -> voice.locale.language == localeLanguage }
                if (voiceGender == "MALE") {
                    Toast.makeText(
                        requireContext(),
                        "No hay voz masculina disponible en este idioma, se usará la voz predeterminada.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            foundVoice?.let { tts.voice = it }
        }
    }



    private fun exportTextToFile() {
        if (processedText.isNullOrEmpty()) return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "text/plain"; putExtra(Intent.EXTRA_TITLE, "MiDocumento")
        }
        createTextFileLauncher.launch(intent)
    }

    private fun exportAudioToFile() {
        if (processedText.isNullOrEmpty() || !isTtsInitialized) return
        applySettingsToTts()
        tts.stop()
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "audio/mpeg"; putExtra(Intent.EXTRA_TITLE, "MiAudio")
        }
        createAudioFileLauncher.launch(intent)
    }

    private fun synthesizeTextToTempFile(destinationUri: Uri) {
        try {
            val tempFile = File.createTempFile("audio_temp", ".wav", requireContext().cacheDir)
            tts.synthesizeToFile(processedText!!, Bundle(), tempFile, EXPORT_AUDIO_UTTERANCE_ID)
            tempFileToCopy = tempFile
            finalAudioUri = destinationUri
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Error al crear archivo temporal.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyTempFileToFinalUri() {
        try {
            tempFileToCopy?.inputStream()?.use { input ->
                requireContext().contentResolver.openOutputStream(finalAudioUri!!)?.use { output -> input.copyTo(output) }
            }
            activity?.runOnUiThread { Toast.makeText(requireContext(), "Archivo de audio guardado.", Toast.LENGTH_SHORT).show() }
        } catch (e: IOException) {
            activity?.runOnUiThread { Toast.makeText(requireContext(), "Error al guardar el archivo de audio.", Toast.LENGTH_SHORT).show() }
        } finally {
            tempFileToCopy?.delete()
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}
