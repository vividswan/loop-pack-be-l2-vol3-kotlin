package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class BrandService(
    private val brandRepository: BrandRepository,
) {
    fun register(name: String, description: String): BrandModel {
        val brand = BrandModel.create(name = name, description = description)
        return brandRepository.save(brand)
    }

    fun getBrand(id: Long): BrandModel {
        return brandRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, BrandErrorCode.NOT_FOUND)
    }

    fun getBrands(): List<BrandModel> {
        return brandRepository.findAll()
    }
}
