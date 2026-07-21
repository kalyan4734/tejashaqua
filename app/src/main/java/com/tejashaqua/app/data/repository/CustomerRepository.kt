package com.tejashaqua.app.data.repository

import com.tejashaqua.app.data.model.CustomerInfo

object CustomerRepository {
    private val customers = listOf(
        CustomerInfo(
            name = "Sowmya S",
            mobile = "8123279974",
            pan = "FBJPS9607C",
            aadhar = "289221408655",
            address = "W/O Srihari K, No 102, 1st Floor, Shirdi Sai Sannidhi Apartment, Near to Anjaneya Temple, Chikkabommansandra, Yelahanka New Town, Bengaluru-5600065",
            about = "She introduced her self as a college lecturer with us. She speaks English very fluently and she came to us with her mother called Nagarathna. We given very small amount and returns on time and joined with us in 3 Lakhs chit. Few months she paid on time amount to us. Suddenly she started health condition technics and she shared doctor reports also with us. From then not paying chit amount and not answering our phone calls. She will respond on only WhatsApp saying next week will send. From last 2 years she is saying same dialogue.",
            imageUrl = null // You can add a local resource ID or URL here
        )
    )

    fun getCustomerBySearch(query: String): CustomerInfo? {
        if (query.lowercase() == "sowmya") {
            return customers.find { it.name.contains("Sowmya", ignoreCase = true) }
        }
        return null
    }
}
