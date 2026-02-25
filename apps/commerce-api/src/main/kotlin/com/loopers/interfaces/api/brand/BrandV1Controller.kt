package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/brands")
class BrandV1Controller(
    private val brandFacade: BrandFacade,
) : BrandV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @RequestBody request: BrandV1Dto.RegisterRequest,
    ): ApiResponse<BrandV1Dto.RegisterResponse> {
        return brandFacade.register(request.name, request.description)
            .let { BrandV1Dto.RegisterResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getBrands(): ApiResponse<List<BrandV1Dto.BrandResponse>> {
        return brandFacade.getBrands()
            .map { BrandV1Dto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
