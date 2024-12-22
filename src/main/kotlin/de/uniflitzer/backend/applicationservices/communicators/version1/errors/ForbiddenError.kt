package de.uniflitzer.backend.applicationservices.communicators.version1.errors

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP

class ForbiddenError(val error: String): HttpClientError(403, "Forbidden", error)