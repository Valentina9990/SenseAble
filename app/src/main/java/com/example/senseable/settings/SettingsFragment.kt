package com.example.senseable.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.senseable.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider

class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var btnLangSpanish: MaterialButton
    private lateinit var btnLangEnglish: MaterialButton
    private lateinit var btnVoiceFemale: MaterialButton
    private lateinit var btnVoiceMale: MaterialButton
    private lateinit var speedSlider: Slider
    private val KEY_LANGUAGE = "key_language"
    private val KEY_VOICE = "key_voice"
    private val KEY_SPEED = "key_speed"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        btnLangSpanish = view.findViewById(R.id.btn_lang_spanish)
        btnLangEnglish = view.findViewById(R.id.btn_lang_english)
        btnVoiceFemale = view.findViewById(R.id.btn_voice_female)
        btnVoiceMale = view.findViewById(R.id.btn_voice_male)
        speedSlider = view.findViewById(R.id.speed_slider)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences("SenseAbleSettings", Context.MODE_PRIVATE)
        loadSettings()
        setupClickListeners()
    }

    private fun loadSettings() {
        val savedLang = sharedPreferences.getString(KEY_LANGUAGE, "es-ES")
        updateLanguageButtons(savedLang ?: "es-ES")

        val savedVoice = sharedPreferences.getString(KEY_VOICE, "FEMALE")
        updateVoiceButtons(savedVoice ?: "FEMALE")

        val savedSpeed = sharedPreferences.getFloat(KEY_SPEED, 1.0f)
        speedSlider.value = savedSpeed
    }

    private fun setupClickListeners() {
        btnLangSpanish.setOnClickListener {
            saveString(KEY_LANGUAGE, "es-ES")
            updateLanguageButtons("es-ES")
        }
        btnLangEnglish.setOnClickListener {
            saveString(KEY_LANGUAGE, "en-US")
            updateLanguageButtons("en-US")
        }

        btnVoiceFemale.setOnClickListener {
            saveString(KEY_VOICE, "FEMALE")
            updateVoiceButtons("FEMALE")
        }
        btnVoiceMale.setOnClickListener {
            saveString(KEY_VOICE, "MALE")
            updateVoiceButtons("MALE")
        }

        // El listener del slider guarda el valor cada vez que cambia
        speedSlider.addOnChangeListener { _, value, _ ->
            saveFloat(KEY_SPEED, value)
        }
    }

    private fun updateLanguageButtons(selectedLang: String) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val activeBg = ContextCompat.getColorStateList(requireContext(), R.color.secondary)
        val inactiveBg = ContextCompat.getColorStateList(requireContext(), R.color.light_primary)

        if (selectedLang == "es-ES") {
            btnLangSpanish.backgroundTintList = activeBg
            btnLangSpanish.setTextColor(activeColor)
            btnLangEnglish.backgroundTintList = inactiveBg
            btnLangEnglish.setTextColor(inactiveColor)
        } else {
            btnLangEnglish.backgroundTintList = activeBg
            btnLangEnglish.setTextColor(activeColor)
            btnLangSpanish.backgroundTintList = inactiveBg
            btnLangSpanish.setTextColor(inactiveColor)
        }
    }

    private fun updateVoiceButtons(selectedVoice: String) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val activeBg = ContextCompat.getColorStateList(requireContext(), R.color.secondary)
        val inactiveBg = ContextCompat.getColorStateList(requireContext(), R.color.light_primary)

        if (selectedVoice == "FEMALE") {
            btnVoiceFemale.backgroundTintList = activeBg
            btnVoiceFemale.setTextColor(activeColor)
            btnVoiceMale.backgroundTintList = inactiveBg
            btnVoiceMale.setTextColor(inactiveColor)
        } else {
            btnVoiceMale.backgroundTintList = activeBg
            btnVoiceMale.setTextColor(activeColor)
            btnVoiceFemale.backgroundTintList = inactiveBg
            btnVoiceFemale.setTextColor(inactiveColor)
        }
    }

    private fun saveString(key: String, value: String) {
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun saveFloat(key: String, value: Float) {
        with(sharedPreferences.edit()) {
            putFloat(key, value)
            apply()
        }
    }
}
