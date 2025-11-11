package com.example.senseable.welcome

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.senseable.MainActivity
import com.example.senseable.R
import com.example.senseable.conversion.ConversionFragment
import com.example.senseable.files.FilesFragment
import com.example.senseable.settings.SettingsFragment


class WelcomeFragment : Fragment() {

    private var selectedImageUri: Uri? = null
    private lateinit var selectFileLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    Toast.makeText(requireContext(), "Archivo seleccionado: $uri", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)

        val btnSelectDocument: Button = view.findViewById(R.id.btnSelectDocument)
        val btnConvertAudio: Button = view.findViewById(R.id.btnConvertAudio)
        val btnHistorial: Button = view.findViewById(R.id.btnHistorial)
        val btnConfiguracion: Button = view.findViewById(R.id.btnConfiguracion)

        btnSelectDocument.setOnClickListener {
            openFileExplorer()
        }

        btnConvertAudio.setOnClickListener {
            if (selectedImageUri != null) {
                Toast.makeText(requireContext(), "Iniciando proceso...", Toast.LENGTH_SHORT).show()
                val conversionFragment = ConversionFragment.newInstance(selectedImageUri!!)
                (activity as? MainActivity)?.navigateTo(conversionFragment)

            } else {
                Toast.makeText(requireContext(), "Por favor, selecciona un documento primero", Toast.LENGTH_SHORT).show()
            }
        }

        btnHistorial.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo historial...", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.navigateTo(FilesFragment())
        }

        btnConfiguracion.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo configuración...", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.navigateTo(SettingsFragment())
        }

        return view
    }

    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" //para abrir el archivo desde download (emulador)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        selectFileLauncher.launch(intent)
    }
}
