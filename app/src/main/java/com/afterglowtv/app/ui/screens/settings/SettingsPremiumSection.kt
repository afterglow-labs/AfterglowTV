package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.dp
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.app.store.amazon.AfterglowIapConfig

internal fun LazyListScope.settingsPremiumSection(
    amazonPremiumEntitled: Boolean,
    amazonPremiumOwnedSku: String?,
    onOpenPremiumPurchase: () -> Unit,
    onRefreshPremiumEntitlements: () -> Unit
) {
    item {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsSectionHeader(
                title = "Premium",
                subtitle = "Amazon-managed subscriptions and lifetime access."
            )
            SettingsRow(
                label = "Status",
                value = if (amazonPremiumEntitled) premiumStatusLabel(amazonPremiumOwnedSku) else "Available"
            )
            ClickableSettingsRow(
                label = "Purchase Premium",
                value = premiumPurchaseSummary(),
                onClick = onOpenPremiumPurchase
            )
            ClickableSettingsRow(
                label = "Refresh purchase status",
                value = "Amazon",
                onClick = onRefreshPremiumEntitlements
            )
        }
    }
}

private fun premiumStatusLabel(sku: String?): String =
    when (sku) {
        AfterglowIapConfig.LIFETIME -> "Lifetime active"
        AfterglowIapConfig.MONTHLY,
        AfterglowIapConfig.TEST_MONTHLY -> "Monthly active"
        AfterglowIapConfig.QUARTERLY -> "Quarterly active"
        AfterglowIapConfig.ANNUAL,
        AfterglowIapConfig.TEST_YEARLY -> "Annual active"
        null -> "Active"
        else -> "Active"
    }

private fun premiumPurchaseSummary(): String {
    val terms = buildList {
        if (BuildConfig.AMAZON_PREMIUM_MONTHLY_SKU.isNotBlank()) add("Monthly")
        if (BuildConfig.AMAZON_PREMIUM_QUARTERLY_SKU.isNotBlank()) add("Quarterly")
        if (BuildConfig.AMAZON_PREMIUM_ANNUALLY_SKU.isNotBlank()) add("Annual")
        if (BuildConfig.AMAZON_PREMIUM_LIFETIME_SKU.isNotBlank()) add("Lifetime")
    }
    return terms.joinToString(" / ")
}
