package com.loopers.application.brand

import com.loopers.domain.brand.BrandModel

data class BrandInfo(
    val id: Long,
    val name: String,
    val description: String,
) {
    companion object {
        fun from(model: BrandModel): BrandInfo {
            return BrandInfo(
                id = model.id,
                name = model.name,
                description = model.description,
            )
        }
    }
}
