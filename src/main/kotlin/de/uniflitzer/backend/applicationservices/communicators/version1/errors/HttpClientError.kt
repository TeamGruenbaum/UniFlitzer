package de.uniflitzer.backend.applicationservices.communicators.version1.errors

sealed class HttpClientError(val statusCode: Int, val name: String, message: String): Exception("$statusCode - $name: $message")