package com.loopers.fixtures

import com.loopers.domain.brand.BrandModel

object BrandTestFixture {

    const val DEFAULT_NAME = "나이키"
    const val DEFAULT_DESCRIPTION = "스포츠 브랜드"

    fun createBrand(
        name: String = DEFAULT_NAME,
        description: String = DEFAULT_DESCRIPTION,
    ): BrandModel {
        return BrandModel.create(name = name, description = description)
    }
}
