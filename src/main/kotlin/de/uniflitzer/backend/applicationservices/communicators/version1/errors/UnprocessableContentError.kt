package de.uniflitzer.backend.applicationservices.communicators.version1.errors

class UnprocessableContentError(val error: String): HttpClientError(422, "Unprocessable Content", error)