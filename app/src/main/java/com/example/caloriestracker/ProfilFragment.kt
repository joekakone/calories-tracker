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
    
    // Inline editing elements
    private lateinit var iconEditName: ImageView
    private lateinit var etEditName: EditText
    private lateinit var btnSaveName: ImageView
    
    private lateinit var iconEditPassword: ImageView
    private lateinit var etEditPassword: EditText
    private lateinit var btnSavePassword: ImageView
    
    private lateinit var iconEditGoal: ImageView
    private lateinit var etEditGoal: EditText
    private lateinit var btnSaveGoal: ImageView
    
    private lateinit var btnLogoutContainer: LinearLayout
    private lateinit var tvCopyright: TextView

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
        
        iconEditName = view.findViewById(R.id.iconEditName)
        etEditName = view.findViewById(R.id.etEditName)
        btnSaveName = view.findViewById(R.id.btnSaveName)
        
        iconEditPassword = view.findViewById(R.id.iconEditPassword)
        etEditPassword = view.findViewById(R.id.etEditPassword)
        btnSavePassword = view.findViewById(R.id.btnSavePassword)
        
        iconEditGoal = view.findViewById(R.id.iconEditGoal)
        etEditGoal = view.findViewById(R.id.etEditGoal)
        btnSaveGoal = view.findViewById(R.id.btnSaveGoal)
        
        btnLogoutContainer = view.findViewById(R.id.btnLogoutContainer)
        tvCopyright = view.findViewById(R.id.tvCopyright)

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

        // Edit Name
        iconEditName.setOnClickListener { enableEditing(etEditName, currentUser?.name ?: "") }
        btnSaveName.setOnClickListener {
            val newName = etEditName.text.toString()
            if (newName.isNotBlank() && newName != currentUser?.name) {
                updateDatabaseField { db -> db.userDao().updateName(currentUserId, newName) }
                disableEditing(etEditName, newName)
            } else {
                disableEditing(etEditName, currentUser?.name ?: "")
            }
        }
        
        // Edit Password
        iconEditPassword.setOnClickListener { enableEditing(etEditPassword, "") } // Keep empty for password
        btnSavePassword.setOnClickListener {
            val newPass = etEditPassword.text.toString()
            if (newPass.isNotBlank()) {
                updateDatabaseField { db -> db.userDao().updatePassword(currentUserId, newPass) }
                disableEditing(etEditPassword, "")
                Toast.makeText(context, "Mot de passe modifié", Toast.LENGTH_SHORT).show()
            } else {
                disableEditing(etEditPassword, "")
            }
        }
        
        // Edit Goal
        iconEditGoal.setOnClickListener { enableEditing(etEditGoal, currentUser?.dailyCalorieGoal?.toString() ?: "2100") }
        btnSaveGoal.setOnClickListener {
            val newGoal = etEditGoal.text.toString().toIntOrNull()
            if (newGoal != null && newGoal > 0) {
                updateDatabaseField { db -> db.userDao().updateDailyGoal(currentUserId, newGoal) }
                disableEditing(etEditGoal, newGoal.toString())
            } else {
                disableEditing(etEditGoal, currentUser?.dailyCalorieGoal?.toString() ?: "2100")
            }
        }

        // Logout
        btnLogoutContainer.setOnClickListener {
            with(sharedPref.edit()) {
                clear()
                apply()
            }
            findNavController().navigate(R.id.action_profil_to_login)
        }
        
        // Footer Link
        tvCopyright.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/joekakone/calories-tracker"))
            startActivity(intent)
        }
    }

    private fun enableEditing(editText: EditText, currentText: String) {
        editText.isEnabled = true
        editText.setText(currentText)
        editText.requestFocus()
        // Selection at end
        editText.setSelection(editText.text.length)
        // Show keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun disableEditing(editText: EditText, text: String) {
        editText.isEnabled = false
        if (editText.inputType != android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD && 
            editText.inputType != (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.setText(text)
        } else {
            editText.setText("")
        }
        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
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
                        etEditName.hint = user.name
                        etEditGoal.hint = "Objectif calorique (${user.dailyCalorieGoal})"
                        
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
        val options = arrayOf("Prendre une photo", "Importer depuis la galerie", "Supprimer la photo")
        AlertDialog.Builder(requireContext())
            .setTitle("Photo de profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePictureLauncher.launch(null)
                    1 -> pickGalleryLauncher.launch("image/*")
                    2 -> removeProfilePicture()
                }
            }
            .show()
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
