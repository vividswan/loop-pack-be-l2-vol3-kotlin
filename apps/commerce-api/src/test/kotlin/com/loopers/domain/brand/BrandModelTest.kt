package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BrandModelTest {

    @DisplayName("브랜드를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("모든 필드가 유효하면, 브랜드가 정상적으로 생성된다.")
        @Test
        fun createsBrand_whenAllFieldsAreValid() {
            // arrange
            val name = "나이키"
            val description = "스포츠 브랜드"

            // act
            val brand = BrandModel.create(name = name, description = description)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo(name) },
                { assertThat(brand.description).isEqualTo(description) },
            )
        }

        @DisplayName("설명이 빈 값이어도, 브랜드가 정상적으로 생성된다.")
        @Test
        fun createsBrand_whenDescriptionIsEmpty() {
            // arrange & act
            val brand = BrandModel.create(name = "나이키", description = "")

            // assert
            assertThat(brand.description).isEmpty()
        }
    }

    @DisplayName("브랜드 이름 검증 시,")
    @Nested
    inner class NameValidation {

        @DisplayName("빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        fun throwsBadRequest_whenNameIsBlank(name: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                BrandModel.create(name = name, description = "설명")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
