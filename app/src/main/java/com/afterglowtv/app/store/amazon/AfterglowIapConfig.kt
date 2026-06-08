package com.afterglowtv.app.store.amazon

object AfterglowIapConfig {
    const val LIFETIME = "com.afterglowtv.app.premium.lifetime.v1"
    const val MONTHLY = "com.afterglowtv.app.premium.monthly.v1"
    const val QUARTERLY = "com.afterglowtv.app.premium.quarterly.v1"
    const val ANNUAL = "com.afterglowtv.app.premium.annually.v1"

    const val TEST_MONTHLY = "com.afterglowtv.app.premium.monthly"
    const val TEST_YEARLY = "com.afterglowtv.app.premium.yearly"

    val productionSkus: Set<String> = setOf(
        LIFETIME,
        MONTHLY,
        QUARTERLY,
        ANNUAL
    )

    val testSkus: Set<String> = setOf(
        TEST_MONTHLY,
        TEST_YEARLY
    )

    val allSkus: Set<String> = productionSkus + testSkus
    val premiumUnlockSkus: Set<String> = allSkus

    fun isLifetimeSku(sku: String): Boolean = sku == LIFETIME

    fun isSubscriptionSku(sku: String): Boolean =
        sku in setOf(MONTHLY, QUARTERLY, ANNUAL, TEST_MONTHLY, TEST_YEARLY)

    fun isPremiumOwned(ownedSkus: Collection<String>): Boolean =
        ownedSkus.any { it in premiumUnlockSkus }
}
