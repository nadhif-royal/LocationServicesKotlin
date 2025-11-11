package com.example.praktikumlocationservices

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.praktikumlocationservices.ui.theme.PraktikumLocationServicesTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        enableEdgeToEdge()
        setContent {
            PraktikumLocationServicesTheme {
                LocationApp(fusedLocationClient)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationApp(
    fusedLocationClient: FusedLocationProviderClient,
    viewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current

    // 1️⃣ Inisialisasi launcher untuk izin lokasi
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.startLocationFetch(fusedLocationClient)
        } else {
            viewModel.updateLocationStateError("Izin lokasi diperlukan untuk melanjutkan.")
        }
    }

    // 2️⃣ Cek izin saat pertama kali composable muncul
    LaunchedEffect(Unit) {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        when {
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED -> {
                viewModel.startLocationFetch(fusedLocationClient)
            }

            else -> {
                locationPermissionLauncher.launch(permission)
            }
        }
    }

    // 3️⃣ UI Berdasarkan State
    val state = viewModel.locationState.value

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Lokasi Saya (Compose)") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            when (state) {
                is LocationState.Idle -> {
                    Text("Menunggu izin lokasi...")
                }

                is LocationState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                is LocationState.Success -> {
                    Text("✅ Lokasi Ditemukan!", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Latitude: ${state.location.latitude}")
                    Text("Longitude: ${state.location.longitude}")
                    Spacer(Modifier.height(16.dp))
                    Text("Alamat:", fontWeight = FontWeight.SemiBold)
                    Text(viewModel.addressResult.value)
                }

                is LocationState.Error -> {
                    Text(
                        text = "❌ Gagal: ${state.message}",
                        color = Color.Red
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh / Minta Ulang Izin Lokasi")
            }
        }
    }
}
