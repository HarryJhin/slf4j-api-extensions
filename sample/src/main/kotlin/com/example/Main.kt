package com.example

import io.github.harryjhin.slf4j.ktx.*

@Slf4j
class OrderService {
    fun process(orderId: String) {
        trace { "processing order: $orderId" }
        info { "order completed: $orderId" }
    }
}

fun main() {
    OrderService().process("ORD-001")
}
