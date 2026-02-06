package com.loopers.support.error

class CoreException(
    val errorType: ErrorType,
    val customMessage: String? = null,
    val errorCode: String? = null,
) : RuntimeException(customMessage ?: errorType.message) {
    constructor(errorType: ErrorType, errorCode: ErrorCode) : this(
        errorType = errorType,
        customMessage = errorCode.message,
        errorCode = errorCode.code,
    )
}
