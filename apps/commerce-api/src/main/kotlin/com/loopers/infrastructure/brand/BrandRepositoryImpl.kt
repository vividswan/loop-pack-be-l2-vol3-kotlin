package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandRepository
import org.springframework.stereotype.Component

@Component
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {

    override fun save(brand: BrandModel): BrandModel {
        return brandJpaRepository.save(brand)
    }

    override fun findById(id: Long): BrandModel? {
        return brandJpaRepository.findById(id).orElse(null)
    }

    override fun findAll(): List<BrandModel> {
        return brandJpaRepository.findAll()
    }
}
