package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brand")
class BrandModel internal constructor(
    name: String,
    description: String,
) : BaseEntity() {

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "description", nullable = false)
    var description: String = description
        protected set

    init {
        validateName(name)
    }

    override fun guard() {
        validateName(name)
    }

    companion object {
        fun create(
            name: String,
            description: String,
        ): BrandModel {
            return BrandModel(
                name = name,
                description = description,
            )
        }

        private fun validateName(name: String) {
            if (name.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, BrandErrorCode.NAME_EMPTY)
            }
        }
    }
}
