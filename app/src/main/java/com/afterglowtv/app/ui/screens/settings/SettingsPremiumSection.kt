package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.dp
import com.afterglowtv.app.BuildConfig

internal fun LazyListScope.settingsPremiumSection(
    amazonPremiumEntitled: Boolean,
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
                value = if (amazonPremiumEntitled) "Active" else "Available"
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

private fun premiumPurchaseSummary(): String {
    val terms = buildList {
        if (BuildConfig.AMAZON_PREMIUM_MONTHLY_SKU.isNotBlank()) add("Monthly")
        if (BuildConfig.AMAZON_PREMIUM_QUARTERLY_SKU.isNotBlank()) add("Quarterly")
        if (BuildConfig.AMAZON_PREMIUM_ANNUALLY_SKU.isNotBlank()) add("Annual")
        if (BuildConfig.AMAZON_PREMIUM_LIFETIME_SKU.isNotBlank()) add("Lifetime")
    }
    return terms.joinToString(" / ")
}
