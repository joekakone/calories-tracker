package com.example.caloriestracker

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ProfilFragment : Fragment() {
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvInitials: TextView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var btnChangePhoto: ImageView
    
    // New Menu Buttons
    private lateinit var btnUpdateInfo: LinearLayout
    private lateinit var btnSecurity: LinearLayout
    private lateinit var btnPreferences: LinearLayout
    private lateinit var btnHelpSupport: LinearLayout
    private lateinit var btnAbout: LinearLayout
    
    private lateinit var btnLogoutContainer: LinearLayout

    private var currentUserId: Int = -1
    private var currentUser: UserEntity? = null
    private var currentProfileUri: String? = null

    // UCrop result launcher
    private val uCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                copyUriToInternalStorageAndDb(resultUri)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(requireContext(), "Erreur de recadrage", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            // Save to temp file and launch uCrop
            val tempFile = File(requireContext().cacheDir, "temp_cam_${UUID.randomUUID()}.png")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            launchUCrop(Uri.fromFile(tempFile))
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(requireContext(), "Permission de la caméra refusée", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            launchUCrop(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profil, container, false)
        
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail)
        tvInitials = view.findViewById(R.id.tvInitials)
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture)
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)
        
        btnUpdateInfo = view.findViewById(R.id.btnUpdateInfo)
        btnSecurity = view.findViewById(R.id.btnSecurity)
        btnPreferences = view.findViewById(R.id.btnPreferences)
        btnHelpSupport = view.findViewById(R.id.btnHelpSupport)
        btnAbout = view.findViewById(R.id.btnAbout)
        
        btnLogoutContainer = view.findViewById(R.id.btnLogoutContainer)

        val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        currentUserId = sharedPref.getInt("user_id", -1)

        loadUserData()

        setupListeners(sharedPref)

        return view
    }

    private fun setupListeners(sharedPref: android.content.SharedPreferences) {
        btnChangePhoto.setOnClickListener { showPhotoOptionsDialog() }
        
        // Fullscreen view
        ivProfilePicture.setOnClickListener { showFullScreenImage() }

        // New Menu Items click listeners (no-ops for now)
        btnUpdateInfo.setOnClickListener {
            Toast.makeText(requireContext(), "Mettre à jour mes informations", Toast.LENGTH_SHORT).show()
        }
        btnSecurity.setOnClickListener {
            Toast.makeText(requireContext(), "Sécurité", Toast.LENGTH_SHORT).show()
        }
        btnPreferences.setOnClickListener {
            Toast.makeText(requireContext(), "Mes préférences", Toast.LENGTH_SHORT).show()
        }
        btnHelpSupport.setOnClickListener {
            Toast.makeText(requireContext(), "Aide et support", Toast.LENGTH_SHORT).show()
        }
        btnAbout.setOnClickListener {
            Toast.makeText(requireContext(), "À propos de nous", Toast.LENGTH_SHORT).show()
        }

        // Logout
        btnLogoutContainer.setOnClickListener {
            with(sharedPref.edit()) {
                clear()
                apply()
            }
            findNavController().navigate(R.id.action_profil_to_login)
        }
    }
    
    private fun updateDatabaseField(action: suspend (AppDatabase) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            action(db)
            withContext(Dispatchers.Main) {
                loadUserData()
            }
        }
    }

    private fun loadUserData() {
        if (currentUserId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(requireContext())
                val user = db.userDao().getUserById(currentUserId)

                withContext(Dispatchers.Main) {
                    if (user != null) {
                        currentUser = user
                        currentProfileUri = user.profilePictureUri
                        
                        tvProfileName.text = user.name
                        tvProfileEmail.text = user.email
                        
                        updateAvatarUI(user.name, user.profilePictureUri)
                    }
                }
            }
        }
    }

    private fun updateAvatarUI(name: String, pictureUri: String?) {
        if (!pictureUri.isNullOrEmpty()) {
            val file = File(pictureUri)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ivProfilePicture.setImageBitmap(bitmap)
                ivProfilePicture.visibility = View.VISIBLE
                tvInitials.visibility = View.GONE
            } else {
                showInitials(name)
            }
        } else {
            showInitials(name)
        }
    }

    private fun showInitials(name: String) {
        ivProfilePicture.visibility = View.GONE
        tvInitials.visibility = View.VISIBLE
        tvInitials.text = getInitials(name)
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split(Regex("\\s+"))
        return if (parts.size >= 2) {
            "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
        } else if (parts.isNotEmpty()) {
            "${parts[0].firstOrNull()?.uppercase() ?: ""}"
        } else {
            ""
        }
    }
    
    private fun showFullScreenImage() {
        if (currentProfileUri.isNullOrEmpty()) return
        
        val file = File(currentProfileUri!!)
        if (!file.exists()) return

        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            setOnClickListener { dialog.dismiss() } // Click to close
        }

        dialog.setContentView(imageView)
        dialog.show()
    }

    private fun showPhotoOptionsDialog() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_profile_photo, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        // Initials and Profile Picture
        val tvBsInitials = bottomSheetView.findViewById<TextView>(R.id.tvBsInitials)
        val ivBsProfilePicture = bottomSheetView.findViewById<ImageView>(R.id.ivBsProfilePicture)
        
        if (!currentProfileUri.isNullOrEmpty()) {
            val file = File(currentProfileUri!!)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ivBsProfilePicture.setImageBitmap(bitmap)
                ivBsProfilePicture.visibility = View.VISIBLE
                tvBsInitials.visibility = View.GONE
            } else {
                ivBsProfilePicture.visibility = View.GONE
                tvBsInitials.visibility = View.VISIBLE
                tvBsInitials.text = getInitials(currentUser?.name ?: "")
            }
        } else {
            ivBsProfilePicture.visibility = View.GONE
            tvBsInitials.visibility = View.VISIBLE
            tvBsInitials.text = getInitials(currentUser?.name ?: "")
        }

        // Close button
        bottomSheetView.findViewById<View>(R.id.btnClose).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Delete photo (Trash icon)
        bottomSheetView.findViewById<View>(R.id.btnDeletePhoto).setOnClickListener {
            removeProfilePicture()
            bottomSheetDialog.dismiss()
        }

        // Camera button
        bottomSheetView.findViewById<View>(R.id.btnBsCamera).setOnClickListener {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                takePictureLauncher.launch(null)
            } else {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            bottomSheetDialog.dismiss()
        }

        // Gallery button
        bottomSheetView.findViewById<View>(R.id.btnBsGallery).setOnClickListener {
            pickGalleryLauncher.launch("image/*")
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun launchUCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "ucrop_${UUID.randomUUID()}.png"))
        
        val options = UCrop.Options()
        options.setCircleDimmedLayer(true)
        options.setShowCropFrame(false)
        options.setShowCropGrid(false)
        
        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .withOptions(options)
            .getIntent(requireContext())
            
        uCropLauncher.launch(uCropIntent)
    }

    private fun copyUriToInternalStorageAndDb(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val filename = "profile_${currentUserId}_${UUID.randomUUID()}.png"
                val file = File(requireContext().filesDir, filename)
                val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                updateDatabaseField { db -> db.userDao().updateProfilePicture(currentUserId, file.absolutePath) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removeProfilePicture() {
        updateDatabaseField { db -> db.userDao().updateProfilePicture(currentUserId, null) }
        Toast.makeText(context, "Photo supprimée", Toast.LENGTH_SHORT).show()
    }
}
