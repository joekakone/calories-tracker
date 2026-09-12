package com.example.caloriestracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {

    private lateinit var viewFinder: PreviewView
    private lateinit var tvHeader: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var btnCapture: ImageView
    private lateinit var tvCaptureInstruction: TextView
    private lateinit var cameraExecutor: ExecutorService
    
    private var imageCapture: ImageCapture? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Permissions requises pour utiliser la caméra", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scanner, container, false)
        viewFinder = view.findViewById(R.id.viewFinder)
        tvHeader = view.findViewById(R.id.tvHeader)
        tvSubtitle = view.findViewById(R.id.tvSubtitle)
        btnCapture = view.findViewById(R.id.btnCapture)
        tvCaptureInstruction = view.findViewById(R.id.tvCaptureInstruction)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        btnCapture.setOnClickListener {
            val imageCapture = imageCapture ?: return@setOnClickListener

            // Create a temporary file
            val photoFile = File(
                requireContext().cacheDir,
                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
            )
            
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("ScannerFragment", "Photo capture failed: ${exc.message}", exc)
                        Toast.makeText(requireContext(), "Erreur lors de la capture", Toast.LENGTH_SHORT).show()
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = android.net.Uri.fromFile(photoFile)
                        val bundle = Bundle().apply {
                            putString("imageUri", savedUri.toString())
                        }
                        findNavController().navigate(R.id.action_scanner_to_meal_summary, bundle)
                    }
                }
            )
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        return view
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                imageCapture = ImageCapture.Builder().build()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("ScannerFragment", "Camera binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
