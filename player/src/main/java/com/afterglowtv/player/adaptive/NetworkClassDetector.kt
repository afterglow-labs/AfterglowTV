package com.afterglowtv.player.adaptive

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks the device's current default network and classifies it into a
 * [NetworkClass]. Used by the bandwidth meter (for initial-bitrate priors)
 * and by the buffer controller (to react to Wi-Fi ↔ Cellular handoff).
 *
 * Registers as a singleton callback on `ConnectivityManager` and stays
 * subscribed for the lifetime of the app — there's no `release()` because
 * we want the priors fresh at every player creation.
 *
 * Permissions: requires no permission for default-network callbacks on
 * API 24+ (we min-target 28). `TelephonyManager.dataNetworkType` requires
 * READ_PHONE_STATE on older APIs but only `READ_BASIC_PHONE_STATE` on 33+,
 * and we gracefully degrade to UNKNOWN if either fails.
 */
@Singleton
class NetworkClassDetector @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkClass = MutableStateFlow(NetworkClass.UNKNOWN)
    val networkClass: StateFlow<NetworkClass> = _networkClass.asStateFlow()

    init {
        registerDefaultNetworkCallback()
        // Resolve the current network once at startup so we don't start in
        // UNKNOWN if the callback fires after the first prepare.
        _networkClass.value = classifyCurrent()
    }

    /** Current best-guess [NetworkClass] for use as an initial bandwidth prior. */
    fun current(): NetworkClass = _networkClass.value

    private fun registerDefaultNetworkCallback() {
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _networkClass.value = classifyNetwork(network)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        _networkClass.value = classifyCapabilities(capabilities)
                    }

                    override fun onLost(network: Network) {
                        _networkClass.value = NetworkClass.UNKNOWN
                    }
                }
            )
        }.onFailure { Log.w(TAG, "registerDefaultNetworkCallback failed", it) }
    }

    private fun classifyCurrent(): NetworkClass {
        val network = connectivityManager.activeNetwork ?: return NetworkClass.UNKNOWN
        return classifyNetwork(network)
    }

    private fun classifyNetwork(network: Network): NetworkClass {
        val caps = runCatching { connectivityManager.getNetworkCapabilities(network) }
            .getOrNull() ?: return NetworkClass.UNKNOWN
        return classifyCapabilities(caps)
    }

    private fun classifyCapabilities(caps: NetworkCapabilities): NetworkClass {
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkClass.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkClass.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> classifyCellular()
            // VPN, Bluetooth, USB — treat as Unknown so we pick the safe Wi-Fi-grade prior.
            else -> NetworkClass.UNKNOWN
        }
    }

    @SuppressLint("MissingPermission")
    private fun classifyCellular(): NetworkClass {
        val telephony = runCatching {
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        }.getOrNull() ?: return NetworkClass.CELLULAR_4G
        val networkType = runCatching {
            telephony.dataNetworkType
        }.getOrNull() ?: return NetworkClass.CELLULAR_4G

        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_NR -> NetworkClass.CELLULAR_5G
            TelephonyManager.NETWORK_TYPE_LTE -> NetworkClass.CELLULAR_4G
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            NETWORK_TYPE_EVDO_0,
            NETWORK_TYPE_EVDO_A,
            NETWORK_TYPE_EVDO_B,
            NETWORK_TYPE_EHRPD -> NetworkClass.CELLULAR_3G
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            NETWORK_TYPE_CDMA,
            NETWORK_TYPE_1XRTT,
            NETWORK_TYPE_IDEN -> NetworkClass.CELLULAR_2G
            else -> NetworkClass.CELLULAR_4G
        }
    }

    companion object {
        private const val TAG = "NetworkClassDetector"
        private const val NETWORK_TYPE_CDMA = 4
        private const val NETWORK_TYPE_EVDO_0 = 5
        private const val NETWORK_TYPE_EVDO_A = 6
        private const val NETWORK_TYPE_1XRTT = 7
        private const val NETWORK_TYPE_IDEN = 11
        private const val NETWORK_TYPE_EVDO_B = 12
        private const val NETWORK_TYPE_EHRPD = 14
    }
}
