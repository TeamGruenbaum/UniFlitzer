package de.uniflitzer.backend.applicationservices.communicators.version1.errors

class ConflictError(val error: String): HttpClientError(409, "Conflict", error)