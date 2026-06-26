package com.authlens.app.core

/**
 * A generic wrapper for any async operation, exposing its current [Status].
 * Used by ViewModels to drive UI state (loading / success / error).
 */
data class Resource<out T>(
    val status: Status,
    val data: T? = null,
    val message: String? = null,
) {
    enum class Status { SUCCESS, ERROR, LOADING }

    val isLoading: Boolean get() = status == Status.LOADING
    val isSuccess: Boolean get() = status == Status.SUCCESS
    val isError: Boolean get() = status == Status.ERROR

    companion object {
        fun <T> success(data: T?): Resource<T> = Resource(Status.SUCCESS, data)
        fun <T> error(message: String, data: T? = null): Resource<T> = Resource(Status.ERROR, data, message)
        fun <T> loading(data: T? = null): Resource<T> = Resource(Status.LOADING, data)
    }
}
