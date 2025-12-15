package com.loopers.interfaces.api

data class ApiResponse<T>(
    val meta: Metadata,
    val data: T?,
) {
    data class Metadata(
        val result: Result,
        val errorCode: String?,
        val message: String?,
    ) {
        enum class Result { SUCCESS, FAIL }

        companion object {
            fun success() = Metadata(Result.SUCCESS, null, null)

            fun fail(errorCode: String, errorMessage: String) = Metadata(Result.FAIL, errorCode, errorMessage)
        }
    }

    companion object {
        fun success(): ApiResponse<Any> = ApiResponse(Metadata.success(), null)

        fun <T> success(data: T? = null) = ApiResponse(Metadata.success(), data)

        fun fail(errorCode: String, errorMessage: String): ApiResponse<Any?> =
            ApiResponse(
                meta = Metadata.fail(errorCode = errorCode, errorMessage = errorMessage),
                data = null,
            )
    }
}
