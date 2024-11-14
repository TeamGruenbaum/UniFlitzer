package de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder

import de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker.UUID
import org.springframework.stereotype.Component

@Component
private class UUIDAnnotationBasedDocumentationInformationAdder private constructor() : AnnotationBasedDocumentationInformationAdder<UUID>(UUID::class, "uuid")
