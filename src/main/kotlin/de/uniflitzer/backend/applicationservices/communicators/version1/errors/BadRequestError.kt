package de.uniflitzer.backend.applicationservices.communicators.version1.errors

class BadRequestError(val errors: List<String>): HttpClientError(400, "Bad Request", errors.toString())