package com.example

class OrderService {
    fun process(orderId: String) {
        trace { "processing order: $orderId" }
        info { "order completed: $orderId" }
    }
}

fun main() {
    val service = OrderService()
    service.process("ORD-001")
}
