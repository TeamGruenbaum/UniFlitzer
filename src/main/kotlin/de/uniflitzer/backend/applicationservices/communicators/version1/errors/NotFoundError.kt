package de.uniflitzer.backend.applicationservices.communicators.version1.errors

class NotFoundError(val error: String): HttpClientError(404, "Not Found", error)