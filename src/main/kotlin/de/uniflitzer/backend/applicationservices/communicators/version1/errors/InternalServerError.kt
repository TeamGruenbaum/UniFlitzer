package de.uniflitzer.backend.applicationservices.communicators.version1.errors

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP

class InternalServerError(val error: String): HttpClientError(500, "Internal Server Error", error)