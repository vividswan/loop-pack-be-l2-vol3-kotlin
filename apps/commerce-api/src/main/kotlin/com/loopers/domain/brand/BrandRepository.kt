package com.loopers.domain.brand

interface BrandRepository {
    fun save(brand: BrandModel): BrandModel
    fun findById(id: Long): BrandModel?
    fun findAll(): List<BrandModel>
}
