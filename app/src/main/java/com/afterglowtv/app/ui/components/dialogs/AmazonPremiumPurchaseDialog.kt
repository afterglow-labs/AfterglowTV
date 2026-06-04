package com.afterglowtv.app.ui.components.dialogs

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.app.store.amazon.AmazonAppstoreBridge
import com.afterglowtv.app.ui.design.AppColors

@Composable
fun AmazonPremiumPurchaseDialog(
    onDismissRequest: () -> Unit
) {
    PremiumDialog(
        title = "Premium Preview Ended",
        subtitle = "Continue with Premium through Amazon.",
        onDismissRequest = onDismissRequest,
        widthFraction = 0.36f,
        heightFraction = null,
        content = {
            Text(
                text = "Your free premium preview has ended. Premium restores the full app experience on this device after Amazon confirms an active subscription or lifetime license.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
            PremiumDialogActionButton(
                label = "Subscribe Monthly",
                emphasized = true,
                onClick = {
                    AmazonAppstoreBridge.requestPremiumPurchase(BuildConfig.AMAZON_PREMIUM_MONTHLY_SKU)
                }
            )
            PremiumDialogActionButton(
                label = "Lifetime License",
                onClick = {
                    AmazonAppstoreBridge.requestPremiumPurchase(BuildConfig.AMAZON_PREMIUM_LIFETIME_SKU)
                }
            )
        },
        footer = {
            PremiumDialogFooterButton(
                label = "Refresh",
                onClick = AmazonAppstoreBridge::refreshEntitlements
            )
            PremiumDialogFooterButton(
                label = "Not Now",
                onClick = onDismissRequest
            )
        }
    )
}
