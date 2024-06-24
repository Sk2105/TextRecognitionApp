package com.sgtech.textscannerapp

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.*
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sgtech.textscannerapp.ui.theme.TextScannerAppTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TextScannerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraView(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun App(modifier: Modifier = Modifier) {
        val result = remember {
            mutableStateOf<String>("")
        }
        val context = LocalContext.current

        val uri: MutableState<Uri?> = remember {
            mutableStateOf<Uri?>(null)
        }

        val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { res ->
                uri.value = res
                scanImageText(context, uri, result)
            }
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {

            if (result.value.isNotEmpty()) {
                Text(text = result.value)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {
                launcher.launch("image/*")
            }) {
                Text(text = "Scan")
            }
        }

    }

    fun scanImageText(context: Context, uri: MutableState<Uri?>, result: MutableState<String>) {
        val inputImage = InputImage.fromFilePath(context, uri.value!!)
        val recognizer =
            com.google.mlkit.vision.text.TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                result.value = visionText.text
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }


    fun checkPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Preview(showBackground = true)
    @Composable
    fun CameraView(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (checkPermission(context)) {
                // Permission granted
                CameraPreviewScreen()

            } else {
                // Permission denied
                Text(text = "Permission denied")
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = {
                    // Request permission
                    requestPermission(context)
                }) {
                    Text(text = "Request Permission")
                }

            }
        }


    }


    @Composable
    fun CameraPreviewScreen() {
        val dialogState = remember { mutableStateOf(false) }
        val owner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val lensFacing = CameraSelector.LENS_FACING_BACK
        val lifecycleOwner = LocalLifecycleOwner.current
        val result = remember {
            mutableStateOf("")
        }
        val showProgressDialog = remember {
            mutableStateOf(false)
        }


        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val cameraProvider = remember { cameraProviderFuture.get() }
        val previewView = remember { PreviewView(context) }
        val imageCapture = remember { Builder().build() }
        val cameraSelector =
            remember { CameraSelector.Builder().requireLensFacing(lensFacing).build() }

        LaunchedEffect(lensFacing) {
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Box(
            contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()
        )
        {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            Button(onClick = {
                showProgressDialog.value = true
                takePicture(context, imageCapture) {
                    result.value = it
                    dialogState.value = true
                    showProgressDialog.value = false
                }
            }) {
                Text(text = "Scan Text")
            }

            if (dialogState.value) {
                AlertDialog(onDismissRequest = { dialogState.value = false },
                    title = {
                        Text(text = "Text Recognition")
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .height(300.dp)
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = result.value)
                        }
                    },

                    confirmButton = {
                        Button(onClick = { dialogState.value = false }) {
                            Text(text = "Close")
                        }
                    }
                )
            }

            if (showProgressDialog.value) {
                AlertDialog(onDismissRequest = { }, confirmButton = { }, text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    )
                    {
                        Text(
                            text = "Scanning...",
                            modifier = Modifier
                                .padding(5.dp)

                        )

                    }

                }, title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    )
                    {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(5.dp)
                        )
                    }

                })
            }

        }


    }


    private fun takePicture(
        context: Context,
        imageCapture: ImageCapture,
        onCompleteListener: (String) -> Unit
    ) {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : OnImageCapturedCallback() {
                @RequiresApi(Build.VERSION_CODES.P)
                @OptIn(ExperimentalGetImage::class)
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    StartTextRecognition(image, onCompleteListener)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Text", "Image capture failed: ${exception.message}")
                    Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    @OptIn(ExperimentalGetImage::class)
    fun StartTextRecognition(
        image: ImageProxy,
        onCompleteListener: (String) -> Unit
    ) {
        Log.d("Text", "StartTextRecognition")
        val recognizer =
            com.google.mlkit.vision.text.TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                onCompleteListener(text)
                Log.d("Text", text)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Log.d("Text", "Failed to process image: $e")
            }
    }


    fun requestPermission(context: Context) {
        val launcher = (context as ComponentActivity).registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission granted
                Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        launcher.launch(android.Manifest.permission.CAMERA)

    }
}