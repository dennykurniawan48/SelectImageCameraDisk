package com.signaltekno.selectimagecameradisk

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.signaltekno.selectimagecameradisk.ui.theme.SelectImageCameraDiskTheme
import kotlinx.coroutines.launch
import java.util.jar.Manifest

@ExperimentalMaterialApi
class MainActivity : ComponentActivity() {
    private val CROP_PIC = 2
    var is_camera_selected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            var imageUri: Uri?  = null
            var bitmap: Bitmap? by remember { mutableStateOf(null)}
            val scaffoldState = rememberScaffoldState()
            SelectImageCameraDiskTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Scaffold(scaffoldState = scaffoldState) {
                        TakePicture(scaffoldState, imageUri, bitmap, {
                            Log.d("Path", "Result Camera Triggered")
                            imageUri = it
                        }){
                            Log.d("Path", "Result Gallery Triggered")
                            bitmap=it
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TakePicture(scaffoldState: ScaffoldState, imageUri: Uri?, bitmap: Bitmap?, setUri: (Uri?)->Unit, setBitmap: (Bitmap?)->Unit) {
        val context = LocalContext.current
        val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
        val coroutineScope = rememberCoroutineScope()

        val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()){ uri: Uri? ->
            setUri.invoke(uri)
            uri?.let {
                setBitmap.invoke(if(Build.VERSION.SDK_INT < 28) MediaStore.Images.Media.getBitmap(context.contentResolver, it) else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                })
            }

            Log.d("Path", "Gallery Selected")
        }

        val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()){ bmp: Bitmap ->
            setUri.invoke(null)
            setBitmap.invoke(bmp)
            Log.d("Path", "Camera Selected")
        }

        val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()){ isGranted: Boolean ->
            if(isGranted){
                if(is_camera_selected) cameraLauncher.launch() else galleryLauncher.launch("image/*")
                coroutineScope.launch {
                    bottomSheetModalState.hide()
                }
            }else{
                coroutineScope.launch {
                    scaffoldState.snackbarHostState.showSnackbar("Permission is not granted.")
                }
            }
        }

        ModalBottomSheetLayout(
            sheetContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(MaterialTheme.colors.primary.copy(0.08f))
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Add Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            color = MaterialTheme.colors.primary,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                        Divider(
                            modifier = Modifier
                                .height(1.dp)
                                .background(MaterialTheme.colors.primary)
                        )
                        Text(text = "From Gallery", modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                is_camera_selected = false
                                when (PackageManager.PERMISSION_GRANTED) {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                                    ) -> {
                                        galleryLauncher.launch("image/*")
                                        coroutineScope.launch {
                                            bottomSheetModalState.hide()
                                        }
                                    }
                                    else -> {
                                        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                }
                            }
                            .padding(15.dp),
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Divider(
                            modifier = Modifier
                                .height(1.dp)
                                .background(MaterialTheme.colors.primary)
                        )
                        Text(text = "From Camera", modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                is_camera_selected = true
                                when (PackageManager.PERMISSION_GRANTED) {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    ) -> {
                                        cameraLauncher.launch()
                                        coroutineScope.launch {
                                            bottomSheetModalState.hide()
                                        }
                                    }
                                    else -> {
                                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }
                            }
                            .padding(15.dp),
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Divider(
                            modifier = Modifier
                                .height(1.dp)
                                .background(MaterialTheme.colors.primary)
                        )
                        Text(text = "Cancel", modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                is_camera_selected = false
                                coroutineScope.launch {
                                    bottomSheetModalState.hide()
                                }
                            }
                            .padding(15.dp),
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            },
            sheetState = bottomSheetModalState,
            sheetShape = RoundedCornerShape(topEnd = 30.dp, topStart = 30.dp),
            modifier = Modifier.background(MaterialTheme.colors.background)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                Button(onClick = {
                    coroutineScope.launch {
                        if (bottomSheetModalState.isVisible) bottomSheetModalState.hide() else bottomSheetModalState.show()
                    }
                }, modifier=Modifier.fillMaxWidth()) {
                    Text(text = "Choose Picture", modifier=Modifier.padding(8.dp), color=Color.White, textAlign = TextAlign.Center)
                }
            }

            bitmap?.let {
                Log.d("Path", "Bitmap OK")
                Image(bitmap = it.asImageBitmap(), contentDescription = "Image", modifier=Modifier.fillMaxWidth().fillMaxHeight(0.45f))
            }
        }
    }

}