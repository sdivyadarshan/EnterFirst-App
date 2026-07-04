package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val party: String, // Paid To / Received From
    val category: String, // Food, Travel, Shopping, Bills, Health, Education, Entertainment, Others
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val paymentStatus: String, // "PAID", "PENDING", "FAILED"
    val paymentMethod: String, // "BANK" or "CASH"
    val merchantUpiId: String? = null,
    val receiptUri: String? = null // For attached receipt image path
)
