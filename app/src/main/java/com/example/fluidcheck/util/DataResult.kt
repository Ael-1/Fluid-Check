package com.example.fluidcheck.util

/**
 * A generic class that holds a value with its loading status.
 * Used by Repositories to pass states and exceptions safely to ViewModels.
 */
sealed class DataResult<out R> {
    data class Success<out T>(val data: T) : DataResult<T>()
    data class Error(val exception: Exception) : DataResult<Nothing>()
    object Loading : DataResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            Loading -> "Loading"
        }
    }
}

/**
 * Extension properties for easier extraction of data.
 */
/**
 * Extension properties for easier extraction of data from [DataResult].
 */
val <T> DataResult<T>.data: T?
    get() = (this as? DataResult.Success)?.data

/**
 * Extension properties for easier extraction of the exception from [DataResult].
 */
val <T> DataResult<T>.exception: Exception?
    get() = (this as? DataResult.Error)?.exception
