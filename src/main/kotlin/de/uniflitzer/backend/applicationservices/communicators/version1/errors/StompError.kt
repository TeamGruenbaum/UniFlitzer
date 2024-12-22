package de.uniflitzer.backend.applicationservices.communicators.version1.errors

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorsDP

class StompError(val errors: List<String>): RuntimeException(errors.toString())