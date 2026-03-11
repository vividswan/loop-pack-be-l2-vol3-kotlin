package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSortType
import com.loopers.domain.product.QProductModel.productModel
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val jpaQueryFactory: JPAQueryFactory,
) : ProductRepository {

    override fun save(product: ProductModel): ProductModel {
        return productJpaRepository.save(product)
    }

    override fun findById(id: Long): ProductModel? {
        return productJpaRepository.findById(id).orElse(null)
    }

    override fun findByIdWithLock(id: Long): ProductModel? {
        return productJpaRepository.findByIdWithLock(id)
    }

    override fun findAllOrderByCreatedAtDesc(): List<ProductModel> {
        return productJpaRepository.findAllByOrderByCreatedAtDesc()
    }

    override fun findAllOrderByPriceAsc(): List<ProductModel> {
        return productJpaRepository.findAllByOrderByPriceAsc()
    }

    override fun findAllOrderByLikeCountDesc(): List<ProductModel> {
        return productJpaRepository.findAllByOrderByLikeCountDesc()
    }

    override fun findProducts(brandId: Long?, sortType: ProductSortType, page: Int, size: Int): Page<ProductModel> {
        val pageable = PageRequest.of(page, size)

        val query = jpaQueryFactory
            .selectFrom(productModel)
            .where(brandId?.let { productModel.brandId.eq(it) })
            .orderBy(toOrderSpecifier(sortType))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())

        val content = query.fetch()

        val countQuery = jpaQueryFactory
            .select(productModel.count())
            .from(productModel)
            .where(brandId?.let { productModel.brandId.eq(it) })

        val totalCount = countQuery.fetchOne() ?: 0L

        return PageImpl(content, pageable, totalCount)
    }

    private fun toOrderSpecifier(sortType: ProductSortType): OrderSpecifier<*> {
        return when (sortType) {
            ProductSortType.LATEST -> productModel.createdAt.desc()
            ProductSortType.PRICE_ASC -> productModel.price.asc()
            ProductSortType.LIKES_DESC -> productModel.likeCount.desc()
        }
    }
}
