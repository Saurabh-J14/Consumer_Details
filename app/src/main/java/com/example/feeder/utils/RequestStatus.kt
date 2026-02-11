package com.example.feeder.data.repository

sealed class RequestStatus<out T> {
    object Waiting : RequestStatus<Nothing>()
    data class Success<T>(val data: T) : RequestStatus<T>()
    data class Error(val message: String) : RequestStatus<Nothing>()
}