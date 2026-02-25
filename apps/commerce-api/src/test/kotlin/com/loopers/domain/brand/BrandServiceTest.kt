package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class BrandServiceTest {

    @Mock
    private lateinit var brandRepository: BrandRepository

    @InjectMocks
    private lateinit var brandService: BrandService

    @DisplayName("브랜드 등록 시,")
    @Nested
    inner class Register {

        @DisplayName("유효한 정보로 등록하면, 브랜드가 저장된다.")
        @Test
        fun savesBrand_whenValidInfoIsProvided() {
            // arrange
            whenever(brandRepository.save(any())).thenAnswer { it.getArgument<BrandModel>(0) }

            // act
            val result = brandService.register("나이키", "스포츠 브랜드")

            // assert
            assertThat(result.name).isEqualTo("나이키")
            assertThat(result.description).isEqualTo("스포츠 브랜드")
        }
    }

    @DisplayName("브랜드 조회 시,")
    @Nested
    inner class GetBrand {

        @DisplayName("존재하는 ID로 조회하면, 브랜드가 반환된다.")
        @Test
        fun returnsBrand_whenIdExists() {
            // arrange
            val brand = BrandModel.create(name = "나이키", description = "스포츠 브랜드")
            whenever(brandRepository.findById(1L)).thenReturn(brand)

            // act
            val result = brandService.getBrand(1L)

            // assert
            assertThat(result.name).isEqualTo("나이키")
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenIdDoesNotExist() {
            // arrange
            whenever(brandRepository.findById(999L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                brandService.getBrand(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드 목록 조회 시,")
    @Nested
    inner class GetBrands {

        @DisplayName("저장된 브랜드 목록이 반환된다.")
        @Test
        fun returnsBrandList() {
            // arrange
            val brands = listOf(
                BrandModel.create(name = "나이키", description = "스포츠"),
                BrandModel.create(name = "아디다스", description = "스포츠"),
            )
            whenever(brandRepository.findAll()).thenReturn(brands)

            // act
            val result = brandService.getBrands()

            // assert
            assertThat(result).hasSize(2)
        }
    }
}
