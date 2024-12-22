package de.uniflitzer.backend.applicationservices.communicators.version1.errors

class StompError(val errors: List<String>): RuntimeException(errors.toString())