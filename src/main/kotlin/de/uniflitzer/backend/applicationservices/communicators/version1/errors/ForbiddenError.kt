package de.uniflitzer.backend.applicationservices.communicators.version1.errors

class ForbiddenError(val error: String): HttpClientError(403, "Forbidden", error)