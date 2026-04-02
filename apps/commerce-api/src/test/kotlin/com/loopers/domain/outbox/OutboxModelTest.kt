package com.loopers.domain.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OutboxModelTest {

    @DisplayName("Outbox 이벤트 생성 시 published는 false이다.")
    @Test
    fun createdOutboxIsUnpublished() {
        // arrange & act
        val outbox = OutboxModel.create(
            aggregateType = "ORDER",
            aggregateId = "1",
            eventType = "ORDER_CREATED",
            payload = """{"orderId":1}""",
            topic = "order-events",
        )

        // assert
        assertThat(outbox.published).isFalse()
        assertThat(outbox.aggregateType).isEqualTo("ORDER")
        assertThat(outbox.topic).isEqualTo("order-events")
    }

    @DisplayName("markPublished 호출 시 published가 true로 변경된다.")
    @Test
    fun markPublishedChangesState() {
        // arrange
        val outbox = OutboxModel.create(
            aggregateType = "PRODUCT",
            aggregateId = "1",
            eventType = "PRODUCT_LIKED",
            payload = "{}",
            topic = "catalog-events",
        )

        // act
        outbox.markPublished()

        // assert
        assertThat(outbox.published).isTrue()
    }
}
