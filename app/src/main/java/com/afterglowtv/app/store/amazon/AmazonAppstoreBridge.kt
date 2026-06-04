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
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dormant Amazon Appstore SDK bridge for Fire builds.
 *
 * Purchase UI stays hidden until the premium-preview free window ends. The bridge refreshes
 * Amazon receipts at startup so post-preview access is controlled by Amazon entitlement.
 */
object AmazonAppstoreBridge : PurchasingListener, LicensingListener {
    private const val TAG = "AmazonAppstoreBridge"
    private const val APPSTORE_AUTHENTICATION_KEY = "AppstoreAuthenticationKey.pem"
    private val initialized = AtomicBoolean(false)
    private val receiptRefreshSawPremium = AtomicBoolean(false)
    private val _premiumEntitled = MutableStateFlow(StorePolicy.isAmazonPremiumEntitledForProcess())
    val premiumEntitled: StateFlow<Boolean> = _premiumEntitled.asStateFlow()

    fun initialize(context: Context) {
        if (!BuildConfig.ENABLE_AMAZON_APPSTORE_SDK) return
        if (!initialized.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        if (!appContext.hasAmazonAppstoreClient()) {
            Log.w(TAG, "Amazon Appstore client not installed; SDK bridge dormant on this device")
            return
        }

        runCatching {
            PurchasingService.registerListener(appContext, this)
            PurchasingService.enablePendingPurchases()
            Log.d(TAG, "Amazon IAP listener registered")
            refreshEntitlements()
        }.onFailure { error ->
            Log.w(TAG, "Unable to register Amazon IAP listener", error)
        }

        if (appContext.hasAppstoreAuthenticationKey()) {
            runCatching {
                LicensingService.verifyLicense(appContext, this)
                Log.d(TAG, "Amazon DRM verification requested")
            }.onFailure { error ->
                Log.w(TAG, "Unable to request Amazon DRM verification", error)
            }
        } else {
            Log.w(TAG, "Amazon DRM key missing; add $APPSTORE_AUTHENTICATION_KEY to the Fire assets before submission")
        }
    }

    fun refreshEntitlements() {
        if (!BuildConfig.ENABLE_AMAZON_APPSTORE_SDK) return
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
        runCatching {
            PurchasingService.purchase(sku)
        }.onFailure { error ->
            Log.w(TAG, "Unable to start Amazon purchase flow for $sku", error)
        }
    }

    override fun onLicenseCommandResponse(licenseResponse: LicenseResponse) {
        Log.d(TAG, "Amazon DRM status: ${licenseResponse.requestStatus}")
    }

    override fun onUserDataResponse(userDataResponse: UserDataResponse) {
        Log.d(TAG, "Amazon user data status: ${userDataResponse.requestStatus}")
    }

    override fun onProductDataResponse(productDataResponse: ProductDataResponse) {
        Log.d(TAG, "Amazon product data status: ${productDataResponse.requestStatus}")
    }

    override fun onPurchaseUpdatesResponse(purchaseUpdatesResponse: PurchaseUpdatesResponse) {
        Log.d(TAG, "Amazon purchase updates status: ${purchaseUpdatesResponse.requestStatus}")
        if (purchaseUpdatesResponse.requestStatus == PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) {
            val receipts = purchaseUpdatesResponse.receipts.orEmpty()
            val hasPremiumReceipt = receipts.hasActivePremiumReceipt()
            if (hasPremiumReceipt) {
                receiptRefreshSawPremium.set(true)
                receipts.notifyFulfilledPremiumReceipts()
                markPremiumEntitlement(true)
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
                purchaseResponse.receipt
                    ?.takeIf { it.isActivePremiumReceipt() }
                    ?.let { receipt ->
                        receipt.notifyFulfilledPremiumReceipt()
                        markPremiumEntitlement(true)
                    }
                refreshEntitlements()
            }
            else -> Unit
        }
    }

    private fun premiumSkus(): Set<String> =
        setOf(
            BuildConfig.AMAZON_PREMIUM_MONTHLY_SKU,
            BuildConfig.AMAZON_PREMIUM_LIFETIME_SKU
        ).filterTo(linkedSetOf()) { it.isNotBlank() }

    private fun markPremiumEntitlement(entitled: Boolean) {
        StorePolicy.setAmazonPremiumEntitledForProcess(entitled)
        _premiumEntitled.value = entitled
        Log.d(TAG, "Amazon premium entitlement active: $entitled")
    }

    private fun Collection<Receipt>.hasActivePremiumReceipt(): Boolean =
        any { it.isActivePremiumReceipt() }

    private fun Collection<Receipt>.notifyFulfilledPremiumReceipts() {
        forEach { it.notifyFulfilledPremiumReceipt() }
    }

    private fun Receipt.isActivePremiumReceipt(): Boolean {
        if (isCanceled) return false
        if (productType != ProductType.ENTITLED && productType != ProductType.SUBSCRIPTION) return false

        val skus = premiumSkus()
        return sku in skus || termSku in skus
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

    private fun Context.hasAppstoreAuthenticationKey(): Boolean =
        try {
            assets.open(APPSTORE_AUTHENTICATION_KEY).use { true }
        } catch (_: IOException) {
            false
        }
}
