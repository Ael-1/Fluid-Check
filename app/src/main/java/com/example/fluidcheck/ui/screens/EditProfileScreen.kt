package com.example.fluidcheck.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.repository.FirestoreRepository
import com.example.fluidcheck.ui.theme.*
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.request.ImageRequest
import com.example.fluidcheck.util.NetworkMonitor
import com.example.fluidcheck.util.ProfilePhotoManager
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.*

private const val ADMIN_EMAIL = "admin@fluidcheck.ai"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    userId: String,
    username: String,
    isAdminMode: Boolean = false,
    repository: UserPreferencesRepository,
    firestoreRepository: FirestoreRepository = remember { FirestoreRepository() },
    authRepository: AuthRepository = remember { AuthRepository() },
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load initial data using userId - Wrap in remember so the flow isn't recreated on every recomposition
    val userRecordFlow = remember(userId) { firestoreRepository.getUserRecordFlow(userId) }
    val currentRecord by userRecordFlow.collectAsState(initial = null)
    
    // Core administrative check
    val isPrimaryAdmin = currentRecord?.email == ADMIN_EMAIL
    
    // Show personal records ONLY if not primary admin AND not currently in admin mode
    val showPersonalRecords = !isPrimaryAdmin && !isAdminMode
    
    // States for profile settings
    var editableUsername by remember(currentRecord) { mutableStateOf(currentRecord?.username ?: username) }
    var editableEmail by remember(currentRecord) { mutableStateOf(currentRecord?.email ?: "") }
    var editablePassword by remember { mutableStateOf("") }
    var editableConfirmPassword by remember { mutableStateOf("") }
    var reauthPassword by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // States for personal records
    var weight by remember(currentRecord) { mutableStateOf(currentRecord?.weight ?: "") }
    var height by remember(currentRecord) { mutableStateOf(currentRecord?.height ?: "") }
    var age by remember(currentRecord) { mutableStateOf(currentRecord?.age ?: "") }
    
    val placeholder = "Please select..."
    var sex by remember(currentRecord) { mutableStateOf(if (currentRecord?.sex?.isEmpty() == true) placeholder else currentRecord?.sex ?: placeholder) }
    var activity by remember(currentRecord) { mutableStateOf(if (currentRecord?.activity?.isEmpty() == true) placeholder else currentRecord?.activity ?: placeholder) }
    var environment by remember(currentRecord) { mutableStateOf(if (currentRecord?.environment?.isEmpty() == true) placeholder else currentRecord?.environment ?: placeholder) }

    val mismatchPasswordErr = stringResource(R.string.error_password_mismatch)
    var showError by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    var showPhotoSourceSelector by remember { mutableStateOf(false) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var permissionType by remember { mutableStateOf("") } // "Camera" or "Photos"

    // Network connectivity tracking
    val networkMonitor = remember { NetworkMonitor(context) }
    val isConnected by networkMonitor.isConnected.collectAsState(initial = true)


    
    // Manual/Optimistic States
    var profilePhotoUrl by remember { mutableStateOf("") }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isPhotoRemoved by remember { mutableStateOf(false) }

    /**
     * Resolves the display model for the profile photo:
     * - Pending cropped Uri (user just picked it)
     * - HTTPS URL (ImgBB) → used directly by Coil
     * - Local file path → wrapped in a File for Coil
     * - Legacy Base64 data URI → treated as empty (user must re-upload)
     */
    val profilePhotoModel: Any? = remember(profilePhotoUrl, pendingPhotoUri, isPhotoRemoved) {
        when {
            isPhotoRemoved -> null
            pendingPhotoUri != null -> pendingPhotoUri
            profilePhotoUrl.startsWith("http") -> profilePhotoUrl
            profilePhotoUrl.isNotEmpty() && !profilePhotoUrl.startsWith("data:") -> File(profilePhotoUrl)
            else -> null // Empty or legacy Base64 — show default icon
        }
    }
    
    // Sync with Firestore - Only overwrite local state if cloud has a valid HTTPS URL
    // and we're not currently in the middle of an upload process.
    LaunchedEffect(currentRecord?.profilePictureUrl) {
        val cloudUrl = currentRecord?.profilePictureUrl ?: ""
        if (cloudUrl.startsWith("http") && !isLoading) {
            profilePhotoUrl = cloudUrl
        }
    }
    
    // One-time initialization: prefer local file (pending upload) > cloud URL
    LaunchedEffect(userId) {
        if (profilePhotoUrl.isEmpty()) {
            val localFile = ProfilePhotoManager.getLocalPhotoFile(context)
            if (localFile != null) {
                profilePhotoUrl = localFile.absolutePath
            } else {
                val cloudUrl = currentRecord?.profilePictureUrl ?: ""
                if (cloudUrl.startsWith("http")) {
                    profilePhotoUrl = cloudUrl
                }
            }
        }
    }


    
    // uCrop Launcher
    val uCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    pendingPhotoUri = resultUri
                    isPhotoRemoved = false
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                android.util.Log.e("EditProfile", "Crop error: ${cropError?.message}")
                scope.launch { snackbarHostState.showSnackbar("Failed to crop image.") }
            }
        }
    )

    fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg"))
        val options = UCrop.Options().apply {
            setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)
            setCompressionQuality(80)
            setHideBottomControls(true)
            setFreeStyleCropEnabled(false) // Keep it as a square
            setToolbarColor(ContextCompat.getColor(context, R.color.primary))
            setStatusBarColor(ContextCompat.getColor(context, R.color.primary))
            setToolbarWidgetColor(ContextCompat.getColor(context, android.R.color.white))
            setCircleDimmedLayer(true) // Show circle preview as it's for profile photo
            setShowCropFrame(true)
            setShowCropGrid(false)
        }

        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
            .withOptions(options)
            .getIntent(context)
        
        uCropLauncher.launch(uCropIntent)
    }
    
    // Camera File Setup
    val photoFile = remember {
        File(context.cacheDir, "profile_photo_temp_${System.currentTimeMillis()}.jpg")
    }
    val photoUri = remember(photoFile) {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                // Convert content:// URI to file:// URI for uCrop compatibility
                val fileUri = Uri.fromFile(photoFile)
                startCrop(fileUri)
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                startCrop(uri)
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                if (permissionType == "Camera") {
                    cameraLauncher.launch(photoUri)
                } else if (permissionType == "Photos") {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            } else {
                val permission = if (permissionType == "Camera") Manifest.permission.CAMERA else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
                    else Manifest.permission.READ_EXTERNAL_STORAGE
                }
                
                val shouldShowRationale = (context as? androidx.activity.ComponentActivity)?.let {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                } ?: false
                
                if (!shouldShowRationale) {
                    showPermissionSettingsDialog = true
                }
            }
        }
    )

    fun requestPermission(type: String) {
        permissionType = type
        val permission = if (type == "Camera") Manifest.permission.CAMERA else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
            else Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            if (type == "Camera") {
                cameraLauncher.launch(photoUri)
            } else {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        } else {
            permissionLauncher.launch(permission)
        }
    }

    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            title = { Text("Permission Required", fontWeight = FontWeight.Bold) },
            text = { Text("$permissionType permission is required to update your display photo. Please enable it in system settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionSettingsDialog = false
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("OPEN SETTINGS", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettingsDialog = false }) {
                    Text("CANCEL", color = TextDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showPhotoSourceSelector) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSourceSelector = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp)
            ) {
                Text(
                    text = "Change Profile Photo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            showPhotoSourceSelector = false
                            requestPermission("Camera")
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.Camera, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Take a Photo", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            showPhotoSourceSelector = false
                            requestPermission("Photos")
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.Image, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Choose from Gallery", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                if (profilePhotoModel != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                showPhotoSourceSelector = false
                                isPhotoRemoved = true
                                pendingPhotoUri = null
                                scope.launch { snackbarHostState.showSnackbar("Photo marked for removal. Save changes to confirm.") }
                            }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Remove Current Photo", fontSize = 16.sp, color = Color.Red, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    fun hasChanges(): Boolean {
        val record = currentRecord ?: UserRecord()
        val photoChanged = pendingPhotoUri != null || (isPhotoRemoved && record.profilePictureUrl.isNotEmpty())
        
        return photoChanged || 
               editableUsername != (record.username.ifEmpty { username }) ||
               editableEmail != record.email ||
               editablePassword.isNotEmpty() ||
               weight != record.weight ||
               height != record.height ||
               age != record.age ||
               sex != (if (record.sex.isEmpty()) placeholder else record.sex) ||
               activity != (if (record.activity.isEmpty()) placeholder else record.activity) ||
               environment != (if (record.environment.isEmpty()) placeholder else record.environment)
    }

    val onAttemptBack = {
        if (hasChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = !isLoading) {
        onAttemptBack()
    }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. Are you sure you want to discard them and go back?") },
            confirmButton = {
                TextButton(onClick = { 
                    showUnsavedChangesDialog = false
                    onBack()
                }) {
                    Text("DISCARD", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) {
                    Text("KEEP EDITING", color = TextDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = { showReauthDialog = false },
            title = { Text("Re-authentication Required", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Please enter your current password to confirm changes to your email or password.")
                    Spacer(modifier = Modifier.height(16.dp))
                    EditField(
                        label = "Current Password",
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        icon = AppIcons.Lock,
                        isPassword = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = authRepository.reauthenticate(reauthPassword)
                            if (result.isSuccess) {
                                showReauthDialog = false
                                showSaveDialog = true
                            } else {
                                snackbarHostState.showSnackbar("Invalid password. Please try again.")
                            }
                            isLoading = false
                        }
                    }
                ) {
                    Text("CONFIRM", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReauthDialog = false }) {
                    Text("CANCEL", color = TextDark)
                }
            }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showSaveDialog = false },
            title = { Text(text = stringResource(R.string.save_changes), fontWeight = FontWeight.Bold) },
            text = { 
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    Text(text = "Are you sure you want to save the changes you've made?")
                }
            },
            confirmButton = {
                if (!isLoading) {
                    @Suppress("DEPRECATION")
                    TextButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                
                                // 0. Handle Photo Upload/Removal first
                                var finalPhotoUrl = profilePhotoUrl
                                if (isPhotoRemoved) {
                                    if (isConnected) {
                                        firestoreRepository.removeProfilePicture(userId)
                                    } else {
                                        // Offline: just clear local photo, Firestore will sync the empty URL via saveUserRecord
                                        context.let { ProfilePhotoManager.deleteLocalPhoto(it) }
                                    }
                                    repository.setPendingPhotoUpload(userId, false)
                                    finalPhotoUrl = ""
                                } else if (pendingPhotoUri != null) {
                                    val uploadResult = firestoreRepository.uploadProfilePicture(userId, pendingPhotoUri!!, isConnected)
                                    if (uploadResult.isSuccess) {
                                        finalPhotoUrl = uploadResult.getOrNull() ?: ""
                                        if (!isConnected) {
                                            repository.setPendingPhotoUpload(userId, true)
                                        } else {
                                            repository.setPendingPhotoUpload(userId, false)
                                        }
                                    } else {
                                        val error = uploadResult.exceptionOrNull()?.message ?: "Unknown error"
                                        snackbarHostState.showSnackbar("Photo upload failed: $error")
                                        isLoading = false
                                        return@launch
                                    }
                                }

                                // Update state for the rest of the flow
                                profilePhotoUrl = finalPhotoUrl
                                pendingPhotoUri = null
                                isPhotoRemoved = false

                                // 1. Check for critical changes that REQUIRE internet
                                val usernameChanged = editableUsername != (currentRecord?.username ?: "")
                                val emailChanged = editableEmail != (currentRecord?.email ?: "")
                                val passwordChanged = editablePassword.isNotEmpty()

                                if (!isConnected && (usernameChanged || emailChanged || passwordChanged)) {
                                    snackbarHostState.showSnackbar("Please connect to the internet to change your username, email, or password.")
                                    isLoading = false
                                    return@launch
                                }

                                // 2. Handle Username Update (Online Only)
                                if (isConnected && usernameChanged) {
                                    if (!firestoreRepository.isUsernameAvailable(editableUsername)) {
                                        snackbarHostState.showSnackbar("This username is already taken. Please try another one.")
                                        isLoading = false
                                        return@launch
                                    }
                                    
                                    val userResult = firestoreRepository.updateUsername(userId, currentRecord?.username ?: "", editableUsername)
                                    if (userResult.isFailure) {
                                        snackbarHostState.showSnackbar(userResult.exceptionOrNull()?.message ?: "Error updating username")
                                        isLoading = false
                                        return@launch
                                    }
                                }

                                // 3. Handle Email Update (Online Only)
                                if (isConnected && emailChanged) {
                                    val emailResult = authRepository.updateEmail(editableEmail)
                                    if (emailResult.isFailure) {
                                        snackbarHostState.showSnackbar("Error updating email. You may need to sign in again.")
                                        isLoading = false
                                        return@launch
                                    }
                                }

                                // 4. Handle Password Update (Online Only)
                                if (isConnected && passwordChanged) {
                                    val passResult = authRepository.updatePassword(editablePassword)
                                    if (passResult.isFailure) {
                                        snackbarHostState.showSnackbar("Error updating password.")
                                        isLoading = false
                                        return@launch
                                    }
                                }

                                // 5. Save User Record
                                val newRecord = (currentRecord ?: UserRecord()).copy(
                                    uid = userId,
                                    username = editableUsername,
                                    email = editableEmail,
                                    weight = weight,
                                    height = height,
                                    age = age,
                                    sex = sex,
                                    activity = activity,
                                    environment = environment,
                                    profilePictureUrl = profilePhotoUrl,
                                    setupCompleted = true
                                )

                                if (isConnected) {
                                    // Online: save to Firestore (batch + await) then local
                                    val saveResult = firestoreRepository.saveUserRecord(userId, newRecord)
                                    if (saveResult.isSuccess) {
                                        repository.saveUserRecord(userId, newRecord)
                                        isLoading = false
                                        showSaveDialog = false
                                        onBack()
                                    } else {
                                        val error = saveResult.exceptionOrNull()?.message ?: "Unknown error"
                                        snackbarHostState.showSnackbar("Error saving changes: $error")
                                        isLoading = false
                                    }
                                } else {
                                    // Offline: save locally only — Firestore will queue non-critical fields and sync when online
                                    repository.saveUserRecord(userId, newRecord)
                                    firestoreRepository.queueOfflineUserRecordUpdate(userId, newRecord)
                                    isLoading = false
                                    showSaveDialog = false
                                    android.widget.Toast.makeText(context, "Changes saved locally! Will sync when online.", android.widget.Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                

                            }
                        }
                    ) {
                        Text(stringResource(R.string.confirm).uppercase(), color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isLoading) {
                    @Suppress("DEPRECATION")
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text(stringResource(R.string.cancel), color = TextDark)
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onAttemptBack, enabled = !isLoading) {
                        Icon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Container
            ProfileSettingsContainer(
                userId = userId,
                username = editableUsername,
                onUsernameChange = { editableUsername = it },
                email = editableEmail,
                onEmailChange = { editableEmail = it },
                password = editablePassword,
                profilePhotoModel = profilePhotoModel,
                onEditPhoto = { showPhotoSourceSelector = true },
                onPasswordChange = { 
                    editablePassword = it 
                    if (it.isEmpty()) {
                        editableConfirmPassword = ""
                        confirmPasswordError = null
                    }
                },
                confirmPassword = editableConfirmPassword,
                onConfirmPasswordChange = { 
                    editableConfirmPassword = it 
                    if (confirmPasswordError != null) confirmPasswordError = null
                },
                confirmPasswordError = confirmPasswordError,
                enabled = !isLoading
            )

            if (showPersonalRecords) {
                Spacer(modifier = Modifier.height(24.dp))

                if (showError) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = "Please fill in all fields and selections.",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Personal Records Container
                PersonalRecordsContainer(
                    weight = weight, onWeightChange = { weight = it },
                    height = height, onHeightChange = { height = it },
                    age = age, onAgeChange = { age = it },
                    sex = sex, onSexChange = { sex = it },
                    activity = activity, onActivityChange = { activity = it },
                    environment = environment, onEnvironmentChange = { environment = it },
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                Button(
                    onClick = {
                        if (showPersonalRecords && (weight.isBlank() || height.isBlank() || age.isBlank() ||
                            sex == placeholder || activity == placeholder || environment == placeholder)) {
                            showError = true
                        } else {
                            showError = false
                            
                            // Check password match
                            if (editablePassword.isNotEmpty() && editablePassword != editableConfirmPassword) {
                                confirmPasswordError = mismatchPasswordErr
                                scope.launch {
                                    scrollState.animateScrollTo(0)
                                }
                                return@Button
                            } else {
                                confirmPasswordError = null
                            }

                            // Check if sensitive changes require reauth
                            if (editableEmail != (currentRecord?.email ?: "") || editablePassword.isNotEmpty()) {
                                showReauthDialog = true
                            } else {
                                showSaveDialog = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    @Suppress("DEPRECATION")
                    Text(stringResource(R.string.save_changes), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ProfileSettingsContainer(
    userId: String,
    username: String,
    onUsernameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    profilePhotoModel: Any? = null,
    onEditPhoto: () -> Unit,
    confirmPasswordError: String? = null,
    enabled: Boolean
) {
    val isGuest = userId == "GUEST"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.profile_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Photo Edit
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue.copy(alpha = 0.2f))
                    ) {
                        if (profilePhotoModel != null) {
                            coil.compose.SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profilePhotoModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                                    }
                                },
                                error = {
                                    Icon(
                                        imageVector = AppIcons.PersonOutline,
                                        contentDescription = null,
                                        modifier = Modifier.padding(24.dp),
                                        tint = PrimaryBlue
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = AppIcons.PersonOutline,
                                contentDescription = null,
                                modifier = Modifier.padding(24.dp),
                                tint = PrimaryBlue
                            )
                        }
                    }
                    SmallFloatingActionButton(
                        onClick = { if (enabled) onEditPhoto() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp),
                        shape = CircleShape,
                        containerColor = if (enabled) PrimaryBlue else Color.Gray,
                        contentColor = Color.White
                    ) {
                        Icon(AppIcons.Camera, contentDescription = "Change photo", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isGuest) {
                EditField(label = stringResource(R.string.username_label), value = username, onValueChange = onUsernameChange, icon = AppIcons.PersonOutline, enabled = enabled)
                Spacer(modifier = Modifier.height(16.dp))
                EditField(label = stringResource(R.string.email_label), value = email, onValueChange = onEmailChange, icon = AppIcons.Email, enabled = enabled)
                Spacer(modifier = Modifier.height(16.dp))
                EditField(label = "New Password", value = password, onValueChange = onPasswordChange, icon = AppIcons.Lock, isPassword = true, enabled = enabled, placeholder = "Leave blank to keep current")
                
                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    EditField(
                        label = stringResource(R.string.confirm_password_label),
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        icon = AppIcons.Lock,
                        isPassword = true,
                        enabled = enabled,
                        error = confirmPasswordError
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRecordsContainer(
    weight: String, onWeightChange: (String) -> Unit,
    height: String, onHeightChange: (String) -> Unit,
    age: String, onAgeChange: (String) -> Unit,
    sex: String, onSexChange: (String) -> Unit,
    activity: String, onActivityChange: (String) -> Unit,
    environment: String, onEnvironmentChange: (String) -> Unit,
    enabled: Boolean
) {
    var sexExpanded by remember { mutableStateOf(false) }
    var actExpanded by remember { mutableStateOf(false) }
    var envExpanded by remember { mutableStateOf(false) }

    val sexOptions = listOf("Male", "Female")
    val activityLevels = listOf("Sedentary", "Lightly Active", "Moderate", "Very Active", "Extra Active")
    val weatherOptions = listOf("Sunny", "Cloudy", "Rainy", "Humid", "Hot", "Cold", "Dry")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.personal_records_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    EditField(label = stringResource(R.string.weight_kg_label), value = weight, onValueChange = onWeightChange, icon = AppIcons.Scale, enabled = enabled)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    EditField(label = stringResource(R.string.height_cm_label), value = height, onValueChange = { onHeightChange(it) }, icon = AppIcons.Height, enabled = enabled)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    EditField(label = stringResource(R.string.age_label), value = age, onValueChange = onAgeChange, icon = AppIcons.Age, enabled = enabled)
                }
                Spacer(modifier = Modifier.width(16.dp))
                
                // Sex Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Column {
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.sex_label),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = sexExpanded && enabled,
                            onExpandedChange = { if (enabled) sexExpanded = !sexExpanded }
                        ) {
                            OutlinedTextField(
                                value = sex,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                ),
                                leadingIcon = { Icon(AppIcons.Gender, contentDescription = null, tint = PrimaryBlue) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                enabled = enabled
                            )
                            ExposedDropdownMenu(
                                expanded = sexExpanded && enabled,
                                onDismissRequest = { sexExpanded = false }
                            ) {
                                sexOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            onSexChange(option)
                                            sexExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Activity Level Dropdown
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.activity_level_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ExposedDropdownMenuBox(
                expanded = actExpanded && enabled,
                onExpandedChange = { if (enabled) actExpanded = !actExpanded }
            ) {
                OutlinedTextField(
                    value = activity,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    leadingIcon = { Icon(AppIcons.Activity, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    enabled = enabled
                )
                ExposedDropdownMenu(
                    expanded = actExpanded && enabled,
                    onDismissRequest = { actExpanded = false }
                ) {
                    activityLevels.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onActivityChange(option)
                                actExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Environment Dropdown
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.environment_dropdown_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ExposedDropdownMenuBox(
                expanded = envExpanded && enabled,
                onExpandedChange = { if (enabled) envExpanded = !envExpanded }
            ) {
                OutlinedTextField(
                    value = environment,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = envExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    leadingIcon = { Icon(AppIcons.Weather, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    enabled = enabled
                )
                ExposedDropdownMenu(
                    expanded = envExpanded && enabled,
                    onDismissRequest = { envExpanded = false }
                ) {
                    weatherOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onEnvironmentChange(option)
                                envExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    isPassword: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    placeholder: String = "",
    error: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        @Suppress("DEPRECATION")
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, fontSize = 14.sp) },
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(icon, contentDescription = null, tint = PrimaryBlue) },
            trailingIcon = {
                if (isPassword) {
                    val image = if (passwordVisible) AppIcons.Visibility else AppIcons.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility")
                    }
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            readOnly = readOnly,
            enabled = enabled,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(text = error, color = Color.Red, fontSize = 12.sp) }
            } else null
        )
    }
}
