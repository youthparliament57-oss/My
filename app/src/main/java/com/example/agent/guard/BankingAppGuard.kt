package com.example.agent.guard

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankingAppGuard @Inject constructor() {
    private val blacklistedPackages = setOf(
        "com.sbi.upi", "com.sbi.yono", "com.hdfcbank.smartbuy", "com.icicibank.mobilebanking",
        "com.axis.mobile", "com.kotak.mobilebanking", "com.phonepe.app", "com.google.android.apps.nbu.paisa.user",
        "com.paytm.payments", "in.org.npci.upiapp", "com.zerodha.kite", "com.nextbillion.groww",
        "com.upstox.pro", "com.amazon.mShop.android.shopping" // Pay is inside Amazon
    )

    fun isProtected(packageName: String): Boolean {
        return blacklistedPackages.contains(packageName)
    }

    fun getBlacklist(): List<String> = blacklistedPackages.toList()
}
