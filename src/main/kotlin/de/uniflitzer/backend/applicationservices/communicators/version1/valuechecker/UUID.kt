package de.uniflitzer.backend.applicationservices.communicators.version1.valuechecker

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass


@Constraint(validatedBy = [UUIDValidator::class])
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class UUID(
    val message: String = "The string is no UUID string",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

private class UUIDValidator private constructor() : ConstraintValidator<UUID?, String?> {
    override fun isValid(value: String?, constraintValidatorContext: ConstraintValidatorContext): Boolean =
        runCatching { java.util.UUID.fromString(value) }.isSuccess
}