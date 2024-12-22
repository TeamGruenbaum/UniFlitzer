package de.uniflitzer.backend.applicationservices.communicators.version1.errors

class InternalServerError(val error: String): HttpClientError(500, "Internal Server Error", error)