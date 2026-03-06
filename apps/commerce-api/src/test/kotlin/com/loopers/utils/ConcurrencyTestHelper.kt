package com.loopers.utils

import org.springframework.http.HttpStatusCode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

object ConcurrencyTestHelper {

    fun executeParallel(
        threadCount: Int,
        action: (Int) -> HttpStatusCode,
    ): List<HttpStatusCode> {
        val executorService = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val results = mutableListOf<HttpStatusCode>()

        repeat(threadCount) { index ->
            executorService.submit {
                try {
                    startLatch.await()
                    val status = action(index)
                    synchronized(results) {
                        results.add(status)
                    }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executorService.shutdown()
        return results
    }
}
