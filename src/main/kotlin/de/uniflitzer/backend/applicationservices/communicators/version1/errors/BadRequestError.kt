package de.uniflitzer.backend.applicationservices.communicators.version1.errors

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorsDP

class BadRequestError(val errorsDP: ErrorsDP): HttpClientError(400, "Bad Request", errorsDP.toString())