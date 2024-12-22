package de.uniflitzer.backend.applicationservices.communicators.version1.errors

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP

class UnprocessableContentError(val error: String): HttpClientError(422, "Unprocessable Content", error)