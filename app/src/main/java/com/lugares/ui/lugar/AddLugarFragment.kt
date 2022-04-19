package com.lugares.ui.lugar

import AudioUtiles
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.lugares.R
import com.lugares.databinding.FragmentAddLugarBinding
import com.lugares.model.Lugar
import com.lugares.utiles.ImagenUtiles
import com.lugares.viewmodel.LugarViewModel

class AddLugarFragment : Fragment() {
  private lateinit var lugarViewModel: LugarViewModel
  private var _binding: FragmentAddLugarBinding? = null
  private val binding get() = _binding!!
  private lateinit var tomarFotoActivity: ActivityResultLauncher<Intent>
  private lateinit var imagenUtiles: ImagenUtiles

  private lateinit var audioUtiles: AudioUtiles


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    lugarViewModel = ViewModelProvider(this).get(LugarViewModel::class.java)

    _binding = FragmentAddLugarBinding.inflate(inflater, container, false)

    binding.btAddLugar.setOnClickListener {
      if (binding.etNombre.text.toString().isNotEmpty()){
        binding.progressBar.visibility = ProgressBar.VISIBLE
        binding.msgMensaje.text = "subiendo nota audio..."
        binding.msgMensaje.visibility = TextView.VISIBLE
        subeAudioNube()
      }else{
        Toast.makeText(requireContext(),"faltan datos...",Toast.LENGTH_LONG).show()
      }
    }





    tomarFotoActivity = registerForActivityResult(
      ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        imagenUtiles.actualizaFoto()
      }
    }


    audioUtiles = AudioUtiles(
      requireActivity(),
      requireContext(),
      binding.btAccion,
      binding.btPlay,
      binding.btDelete,
      getString(R.string.msg_graba_audio),
      getString(R.string.msg_detener_audio),
    )



    imagenUtiles = ImagenUtiles(
      requireContext(), binding.btPhoto, binding.btRotaL, binding.btRotaR,
      binding.imagen, tomarFotoActivity
    )



    ubicaGPS()

    return binding.root
  }

  private var conPermisos: Boolean = true;
  private fun ubicaGPS() {
    val fusedLocationProviderClient: FusedLocationProviderClient =
      LocationServices.getFusedLocationProviderClient(requireContext())
    if (ActivityCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_FINE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED
      && ActivityCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(
        requireActivity(), arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        ), 105
      )
    }
    if (conPermisos) {
      fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
        if (location != null) {
          binding.tvLatitud.text = "${location.latitude}"
          binding.tvLongitud.text = "${location.longitude}"
          binding.tvAltura.text = "${location.altitude}"
        } else {
          binding.tvLatitud.text = getString(R.string.error)
          binding.tvLongitud.text = getString(R.string.error)
          binding.tvAltura.text = getString(R.string.error)
        }
      }
    }
  }


  private fun agregarLugar(rutaAudio: String, rutaImagen: String) {
    val nombre = binding.etNombre.text.toString()
    if (nombre.isNotEmpty()) {
      val correo = binding.etCorreo.text.toString()
      val telefono = binding.etTelefono.text.toString()
      val web = binding.etWeb.text.toString()
      val latitud = binding.tvLatitud.text.toString().toDouble()
      val longitud = binding.tvLongitud.text.toString().toDouble()
      val altura = binding.tvAltura.text.toString().toDouble()

      val lugar = Lugar(
        "", nombre, correo, telefono, web,
        latitud, longitud, altura,
        rutaAudio, rutaImagen
      )

      binding.progressBar.visibility = ProgressBar.GONE
      lugarViewModel.addLugar(lugar)
      Toast.makeText(
        requireContext(),
        getString(R.string.msg_lugar_add),
        Toast.LENGTH_SHORT
      ).show()
      findNavController().navigate(R.id.action_addLugarFragment_to_nav_lugar)
    }
  }

  private fun subeAudioNube() {
    val audioFile = audioUtiles.audioFile
    if (audioFile.exists() && audioFile.isFile && audioFile.canRead()) {
      val ruta = Uri.fromFile(audioFile)
      val referencia: StorageReference = //Referenciahacia el storage firebase
        Firebase.storage.reference
          .child("lugaresApp/${Firebase.auth.currentUser?.uid}/audios/${audioFile.name}")
      val uploadTask = referencia.putFile(ruta)
      uploadTask.addOnSuccessListener {
        val douloadUrl = referencia.downloadUrl
        douloadUrl.addOnSuccessListener {
          val rutaNota = it.toString()
          subeImagenNube(rutaNota)
        }
      }
      uploadTask.addOnFailureListener {
        Toast.makeText(context, "Error subiendo nota", Toast.LENGTH_LONG).show()
        subeImagenNube("")
      }
    } else {
      Toast.makeText(context, "no se sube Nota de audio", Toast.LENGTH_LONG).show()
      subeImagenNube("")
    }
  }

  private fun subeImagenNube(rutaAudio: String) {
    binding.msgMensaje.text = "subiendo imagen..."
    val imagenFile = imagenUtiles.imagenFile
    if (imagenFile.exists() && imagenFile.isFile && imagenFile.canRead()) {
      val ruta = Uri.fromFile(imagenFile)
      val referencia: StorageReference = //Referencia hacia el storage de firebase
        Firebase.storage.reference
          .child("lugaresApp/${Firebase.auth.currentUser?.uid.toString()}/imagenes/${imagenFile.name}")
      val uploadTask = referencia.putFile(ruta)
      uploadTask.addOnSuccessListener {
        val douloadUrl = referencia.downloadUrl
        douloadUrl.addOnSuccessListener {
          val rutaImagen = it.toString()
          agregarLugar(rutaAudio, rutaImagen)
        }
      }
      uploadTask.addOnFailureListener {
        Toast.makeText(context, "Error subiendo imagen", Toast.LENGTH_LONG).show()
        agregarLugar(rutaAudio, "")
      }
    } else {
      Toast.makeText(context, "no se sube imagen", Toast.LENGTH_LONG).show()
      agregarLugar(rutaAudio, "")
    }
  }


  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
