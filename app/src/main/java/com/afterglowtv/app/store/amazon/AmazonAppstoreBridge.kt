package com.afterglowtv.app.store.amazon

import android.content.Context
import android.util.Log
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.app.store.StorePolicy
import com.amazon.device.drm.LicensingListener
import com.amazon.device.drm.LicensingService
import com.amazon.device.drm.model.LicenseResponse
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.ProductType
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserDataResponse
import com.amazon.device.iap.model.FulfillmentResult
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Amazon Appstore SDK bridge for Fire builds.
 */
object AmazonAppstoreBridge : PurchasingListener, LicensingListener {
    private const val TAG = "AmazonAppstoreBridge"
    private const val PREMIUM_PREFS = "afterglow_amazon_premium"
    private const val KEY_PREMIUM_ENTITLED = "premium_entitled"
    private const val KEY_PREMIUM_SKU = "premium_sku"
    private val iapInitialized = AtomicBoolean(false)
    private val drmVerificationStarted = AtomicBoolean(false)
    private val receiptRefreshSawPremium = AtomicBoolean(false)
    private val _premiumEntitled = MutableStateFlow(StorePolicy.isAmazonPremiumEntitledForProcess())
    private val _premiumOwnedSku = MutableStateFlow<String?>(null)
    @Volatile private var appContext: Context? = null
    val premiumEntitled: StateFlow<Boolean> = _premiumEntitled.asStateFlow()
    val premiumOwnedSku: StateFlow<String?> = _premiumOwnedSku.asStateFlow()

    fun attach(context: Context) {
        if (!BuildConfig.ENABLE_AMAZON_APPSTORE_SDK) return
        val applicationContext = context.applicationContext
        appContext = applicationContext
        restoreCachedPremiumState(applicationContext)
        verifyAmazonLicenseOrExit()
        refreshEntitlements()
    }

    fun initialize(context: Context) {
        attach(context)
    }

    private fun verifyAmazonLicenseOrExit() {
        if (!BuildConfig.ENABLE_AMAZON_DRM_LICENSING) return
        val context = appContext ?: return
        if (!context.hasAmazonAppstoreClient()) {
            Log.e(TAG, "Amazon DRM licensing required, but no Amazon Appstore or SDK Tester client is installed")
            terminateUnlicensedApp()
            return
        }
        if (!drmVerificationStarted.compareAndSet(false, true)) return

        runCatching {
            val requestId = LicensingService.verifyLicense(context, this)
            Log.i(TAG, "Amazon DRM license verification requested: $requestId")
        }.onFailure { error ->
            drmVerificationStarted.set(false)
            Log.e(TAG, "Unable to start Amazon DRM license verification", error)
            terminateUnlicensedApp()
        }
    }

    private fun ensureIapRegistered(): Boolean {
        if (!BuildConfig.ENABLE_AMAZON_APPSTORE_SDK) return false
        val context = appContext
        if (context == null) {
            Log.w(TAG, "Amazon Appstore SDK bridge has no application context; skipping IAP call")
            return false
        }
        if (!context.hasAmazonAppstoreClient()) {
            Log.w(TAG, "Amazon Appstore client not installed; SDK bridge dormant on this device")
            return false
        }
        if (!iapInitialized.compareAndSet(false, true)) return true

        val registered = runCatching {
            PurchasingService.registerListener(context, this)
            PurchasingService.enablePendingPurchases()
            Log.d(TAG, "Amazon IAP listener registered")
            true
        }.onFailure { error ->
            iapInitialized.set(false)
            Log.w(TAG, "Unable to register Amazon IAP listener", error)
        }.getOrDefault(false)
        return registered
    }

    fun refreshEntitlements() {
        if (!ensureIapRegistered()) return
        runCatching {
            receiptRefreshSawPremium.set(false)
            PurchasingService.getUserData()
            PurchasingService.getPurchaseUpdates(true)
            premiumSkus().takeIf { it.isNotEmpty() }?.let(PurchasingService::getProductData)
        }.onFailure { error ->
            Log.w(TAG, "Unable to refresh Amazon purchase state", error)
        }
    }

    fun requestPremiumPurchase(sku: String) {
        if (!BuildConfig.ENABLE_AMAZON_APPSTORE_SDK) return
        if (sku !in premiumSkus()) {
            Log.w(TAG, "Ignoring unknown Amazon premium SKU: $sku")
            return
        }
        if (!ensureIapRegistered()) return
        runCatching {
            PurchasingService.purchase(sku)
        }.onFailure { error ->
            Log.w(TAG, "Unable to start Amazon purchase flow for $sku", error)
        }
    }

    override fun onUserDataResponse(userDataResponse: UserDataResponse) {
        Log.d(TAG, "Amazon user data status: ${userDataResponse.requestStatus}")
    }

    override fun onLicenseCommandResponse(licenseResponse: LicenseResponse) {
        when (val status = licenseResponse.requestStatus) {
            LicenseResponse.RequestStatus.LICENSED -> {
                Log.i(TAG, "Amazon DRM license verified: ${licenseResponse.requestId}")
            }
            LicenseResponse.RequestStatus.NOT_LICENSED,
            LicenseResponse.RequestStatus.ERROR_VERIFICATION,
            LicenseResponse.RequestStatus.ERROR_INVALID_LICENSING_KEYS,
            LicenseResponse.RequestStatus.EXPIRED,
            LicenseResponse.RequestStatus.UNKNOWN_ERROR -> {
                Log.e(TAG, "Amazon DRM license denied: status=$status request=${licenseResponse.requestId}")
                terminateUnlicensedApp()
            }
        }
    }

    override fun onProductDataResponse(productDataResponse: ProductDataResponse) {
        Log.d(TAG, "Amazon product data status: ${productDataResponse.requestStatus}")
    }

    override fun onPurchaseUpdatesResponse(purchaseUpdatesResponse: PurchaseUpdatesResponse) {
        Log.d(TAG, "Amazon purchase updates status: ${purchaseUpdatesResponse.requestStatus}")
        if (purchaseUpdatesResponse.requestStatus == PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) {
            val receipts = purchaseUpdatesResponse.receipts.orEmpty()
            val premiumReceipt = receipts.firstActivePremiumReceipt()
            if (premiumReceipt != null) {
                receiptRefreshSawPremium.set(true)
                receipts.notifyFulfilledPremiumReceipts()
                markPremiumEntitlement(true, premiumReceipt.matchedPremiumSku())
            }
            if (purchaseUpdatesResponse.hasMore()) {
                runCatching {
                    PurchasingService.getPurchaseUpdates(false)
                }.onFailure { error ->
                    Log.w(TAG, "Unable to continue Amazon purchase update pagination", error)
                }
            } else if (!receiptRefreshSawPremium.getAndSet(false)) {
                markPremiumEntitlement(false)
            }
        }
    }

    override fun onPurchaseResponse(purchaseResponse: PurchaseResponse) {
        Log.d(TAG, "Amazon purchase response status: ${purchaseResponse.requestStatus}")
        when (purchaseResponse.requestStatus) {
            PurchaseResponse.RequestStatus.SUCCESSFUL,
            PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                val premiumReceipt = purchaseResponse.receipt?.takeIf { it.isActivePremiumReceipt() }
                if (premiumReceipt != null) {
                    premiumReceipt.notifyFulfilledPremiumReceipt()
                    markPremiumEntitlement(true, premiumReceipt.matchedPremiumSku())
                } else {
                    refreshEntitlements()
                }
            }
            else -> Unit
        }
    }

    private fun premiumSkus(): Set<String> =
        (AfterglowIapConfig.premiumUnlockSkus + setOf(
            BuildConfig.AMAZON_PREMIUM_MONTHLY_SKU,
            BuildConfig.AMAZON_PREMIUM_QUARTERLY_SKU,
            BuildConfig.AMAZON_PREMIUM_ANNUALLY_SKU,
            BuildConfig.AMAZON_PREMIUM_LIFETIME_SKU
        )).filterTo(linkedSetOf()) { it.isNotBlank() }

    private fun markPremiumEntitlement(entitled: Boolean, ownedSku: String? = null) {
        StorePolicy.setAmazonPremiumEntitledForProcess(entitled)
        _premiumEntitled.value = entitled
        _premiumOwnedSku.value = ownedSku.takeIf { entitled }
        appContext?.getSharedPreferences(PREMIUM_PREFS, Context.MODE_PRIVATE)?.edit()?.apply {
            putBoolean(KEY_PREMIUM_ENTITLED, entitled)
            if (entitled && !ownedSku.isNullOrBlank()) {
                putString(KEY_PREMIUM_SKU, ownedSku)
            } else if (!entitled) {
                remove(KEY_PREMIUM_SKU)
            }
            apply()
        }
        Log.d(TAG, "Amazon premium entitlement active: $entitled")
    }

    private fun restoreCachedPremiumState(context: Context) {
        val prefs = context.getSharedPreferences(PREMIUM_PREFS, Context.MODE_PRIVATE)
        val entitled = prefs.getBoolean(KEY_PREMIUM_ENTITLED, false)
        val sku = prefs.getString(KEY_PREMIUM_SKU, null)
        StorePolicy.setAmazonPremiumEntitledForProcess(entitled)
        _premiumEntitled.value = entitled
        _premiumOwnedSku.value = sku.takeIf { entitled }
    }

    private fun Collection<Receipt>.firstActivePremiumReceipt(): Receipt? =
        firstOrNull { it.isActivePremiumReceipt() }

    private fun Collection<Receipt>.notifyFulfilledPremiumReceipts() {
        forEach { it.notifyFulfilledPremiumReceipt() }
    }

    private fun Receipt.matchedPremiumSku(): String? {
        val skus = premiumSkus()
        return when {
            sku in skus -> sku
            termSku in skus -> termSku
            else -> null
        }
    }

    private fun Receipt.isActivePremiumReceipt(): Boolean {
        if (isCanceled) return false
        if (productType != ProductType.ENTITLED && productType != ProductType.SUBSCRIPTION) return false

        return matchedPremiumSku() != null
    }

    private fun Receipt.notifyFulfilledPremiumReceipt() {
        if (!isActivePremiumReceipt() || productType != ProductType.ENTITLED) return
        val receiptId = this.receiptId.takeUnless { it.isBlank() } ?: return
        runCatching {
            PurchasingService.notifyFulfillment(receiptId, FulfillmentResult.FULFILLED)
        }.onFailure { error ->
            Log.w(TAG, "Unable to notify Amazon fulfillment for $receiptId", error)
        }
    }

    private fun Context.hasAmazonAppstoreClient(): Boolean =
        listOf("com.amazon.venezia", "com.amazon.sdktestclient")
            .any { packageName ->
                runCatching {
                    packageManager.getPackageInfo(packageName, 0) != null
                }.getOrDefault(false)
            }

    private fun terminateUnlicensedApp(): Nothing {
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(1)
    }
}
