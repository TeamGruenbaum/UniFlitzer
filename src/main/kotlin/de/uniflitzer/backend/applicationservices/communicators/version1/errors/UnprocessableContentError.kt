package de.uniflitzer.backend.applicationservices.communicators.version1.errors

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP

class UnprocessableContentError(val errorDP: ErrorDP): HttpClientError(422, "Unprocessable Content", errorDP.toString())