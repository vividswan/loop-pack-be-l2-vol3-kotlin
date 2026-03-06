package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class LikeModelTest {

    @DisplayName("좋아요를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("회원 ID와 상품 ID로 좋아요가 정상적으로 생성된다.")
        @Test
        fun createsLike_whenMemberIdAndProductIdAreProvided() {
            // arrange
            val memberId = 1L
            val productId = 2L

            // act
            val like = LikeModel.create(memberId = memberId, productId = productId)

            // assert
            assertAll(
                { assertThat(like.memberId).isEqualTo(memberId) },
                { assertThat(like.productId).isEqualTo(productId) },
            )
        }
    }
}
