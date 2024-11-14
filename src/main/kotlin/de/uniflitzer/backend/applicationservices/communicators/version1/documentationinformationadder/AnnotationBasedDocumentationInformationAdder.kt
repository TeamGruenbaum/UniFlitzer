package de.uniflitzer.backend.applicationservices.communicators.version1.documentationinformationadder

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import org.springdoc.core.customizers.ParameterCustomizer
import org.springdoc.core.customizers.PropertyCustomizer
import org.springframework.core.MethodParameter
import kotlin.reflect.KClass

abstract class AnnotationBasedDocumentationInformationAdder<T : Annotation>(
    private val annotation: KClass<T>,
    private val format: String
) : PropertyCustomizer, ParameterCustomizer {
    override fun customize(property: Schema<*>, type: AnnotatedType?): Schema<*> {
        type?.ctxAnnotations?.find { it.annotationClass == annotation }?.let {
            property.format = format
        }

        return property
    }

    override fun customize(parameter: Parameter?, methodParameter: MethodParameter): Parameter? {
        methodParameter.parameterAnnotations.find { it.annotationClass == annotation }?.let {
            parameter?.schema?.format = format
        }

        return parameter
    }
}