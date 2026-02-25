package com.loopers.application.brand

import com.loopers.domain.brand.BrandService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandFacade(
    private val brandService: BrandService,
) {
    @Transactional
    fun register(name: String, description: String): BrandInfo {
        val brand = brandService.register(name, description)
        return BrandInfo.from(brand)
    }

    @Transactional(readOnly = true)
    fun getBrands(): List<BrandInfo> {
        return brandService.getBrands().map { BrandInfo.from(it) }
    }
}
