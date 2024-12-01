package de.uniflitzer.backend.applicationservices.communicators.version1.errors

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP

class ForbiddenError(val errorDP: ErrorDP): HttpClientError(403, "Forbidden", errorDP.toString())