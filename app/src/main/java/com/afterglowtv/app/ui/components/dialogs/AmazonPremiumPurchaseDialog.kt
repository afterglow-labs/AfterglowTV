package com.afterglowtv.app.ui.components.dialogs

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.store.amazon.AfterglowIapConfig
import com.afterglowtv.app.store.amazon.AmazonAppstoreBridge
import com.afterglowtv.app.ui.design.AppColors

@Composable
fun AmazonPremiumPurchaseDialog(
    onDismissRequest: () -> Unit,
    title: String = "Premium Preview Ended",
    subtitle: String = "Continue with Premium through Amazon.",
    message: String = "Your free premium preview has ended. Premium restores the full app experience on this device after Amazon confirms an active subscription or lifetime license."
) {
    PremiumDialog(
        title = title,
        subtitle = subtitle,
        onDismissRequest = onDismissRequest,
        widthFraction = 0.36f,
        heightFraction = null,
        content = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
            PremiumDialogActionButton(
                label = "Subscribe Monthly",
                emphasized = true,
                onClick = {
                    AmazonAppstoreBridge.requestPremiumPurchase(AfterglowIapConfig.MONTHLY)
                }
            )
            PremiumDialogActionButton(
                label = "Subscribe Quarterly",
                onClick = {
                    AmazonAppstoreBridge.requestPremiumPurchase(AfterglowIapConfig.QUARTERLY)
                }
            )
            PremiumDialogActionButton(
                label = "Subscribe Annually",
                onClick = {
                    AmazonAppstoreBridge.requestPremiumPurchase(AfterglowIapConfig.ANNUAL)
                }
            )
            PremiumDialogActionButton(
                label = "Lifetime License",
                onClick = {
                    AmazonAppstoreBridge.requestPremiumPurchase(AfterglowIapConfig.LIFETIME)
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
