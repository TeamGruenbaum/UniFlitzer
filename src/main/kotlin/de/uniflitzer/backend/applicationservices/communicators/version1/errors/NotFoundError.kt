package de.uniflitzer.backend.applicationservices.communicators.version1.errors

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP

class NotFoundError(val errorDP: ErrorDP): HttpClientError(404, "Not Found", errorDP.toString())