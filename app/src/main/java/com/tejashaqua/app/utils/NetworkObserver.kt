package com.tejashaqua.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

import kotlinx.coroutines.delay

/**
 * Robust network observer that tracks the actual internet connectivity status.
 * It uses the default network callback to ensure we only respond to changes
 * in the network that the system has selected for the application.
 */
class NetworkObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val observe: Flow<Status> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                updateStatus()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, capabilities)
                updateStatus()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // When a network is lost, we check if there's any other fallback network
                // before immediately reporting as Lost.
                updateStatus(isLosing = true)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                updateStatus()
            }

            private fun updateStatus(isLosing: Boolean = false) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                
                // We check for any active transport (WiFi or Cellular) 
                // to avoid flickering when internet is poor but connection exists.
                val hasConnection = capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )

                launch {
                    if (!hasConnection && isLosing) {
                        // Small delay before confirming loss to handle network handovers
                        delay(2000)
                        val retryCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                        val stillNoConnection = retryCapabilities == null || !(
                                retryCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                retryCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                retryCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        )
                        
                        if (stillNoConnection) {
                            send(Status.Lost)
                        }
                    } else {
                        send(if (hasConnection) Status.Available else Status.Lost)
                    }
                }
            }
        }

        // registerDefaultNetworkCallback is available from API 24+
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            // Fallback for older versions or issues
        }
        
        // Initial state check
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isInitiallyConnected = capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )
        
        launch { 
            send(if (isInitiallyConnected) Status.Available else Status.Lost)
        }

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {}
        }
    }.distinctUntilChanged()

    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}
