package de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder

import jakarta.validation.constraints.Email
import org.springframework.stereotype.Component

@Component
class EmailAnnotationBasedDocumentationInformationAdder private constructor(): AnnotationBasedDocumentationInformationAdder<Email>(Email::class, "email")