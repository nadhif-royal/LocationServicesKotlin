package com.example.praktikumlocationservices

import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

// 1. Data class untuk menyimpan hasil lokasi
data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "Menunggu lokasi..."
)

// 2. Sealed class untuk state UI
sealed class LocationState {
    data object Idle : LocationState()
    data object Loading : LocationState()
    data class Success(val location: LocationData) : LocationState()
    data class Error(val message: String) : LocationState()
}

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val _locationState = mutableStateOf<LocationState>(LocationState.Idle)
    val locationState: State<LocationState> = _locationState

    // Tempat untuk menyimpan alamat yang ditemukan
    private val _addressResult = mutableStateOf("Mencari alamat...")
    val addressResult: State<String> = _addressResult

    fun updateLocationStateError(errorMessage: String) {
        _locationState.value = LocationState.Error(errorMessage)
    }

    // Fungsi untuk mengubah state menjadi Loading dan memicu pengambilan lokasi
    fun startLocationFetch(fusedLocationClient: FusedLocationProviderClient) {
        _locationState.value = LocationState.Loading
        getLastKnownLocation(fusedLocationClient)
    }

    // Mengambil lokasi terakhir yang diketahui
    @Suppress("MissingPermission")
    private fun getLastKnownLocation(fusedLocationClient: FusedLocationProviderClient) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    _locationState.value = LocationState.Success(locationData)

                    // Panggil Reverse Geocoding di ViewModelScope
                    viewModelScope.launch {
                        val context = getApplication<Application>().applicationContext
                        val address = getAddressFromCoordinates(context, location)
                        _addressResult.value = address
                    }
                } else {
                    // Lokasi tidak ditemukan (misal GPS mati atau belum ada lokasi tersimpan)
                    _locationState.value = LocationState.Error("Lokasi tidak ditemukan.")
                }
            }
            .addOnFailureListener { e ->
                _locationState.value = LocationState.Error("Gagal mengambil lokasi: ${e.message}")
            }
    }

    // Fungsi suspend untuk Reverse Geocoding (dijalankan di IO Dispatcher)
    private suspend fun getAddressFromCoordinates(
        context: Context,
        location: Location
    ): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0) ?: "Alamat tidak dapat diurai"
            } else {
                "Alamat tidak ditemukan (Internet mungkin bermasalah)"
            }
        } catch (e: IOException) {
            "Koneksi geocoding gagal (tidak ada internet)"
        } catch (e: Exception) {
            "Kesalahan geocoding: ${e.message}"
        }
    }
}
