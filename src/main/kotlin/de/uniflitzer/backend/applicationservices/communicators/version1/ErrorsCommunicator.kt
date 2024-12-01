package de.uniflitzer.backend.applicationservices.communicators.version1

import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorsDP
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.*
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ErrorsCommunicator {
    @ExceptionHandler(ForbiddenError::class)
    fun handleForbiddenErrors(forbiddenError: ForbiddenError): ResponseEntity<ErrorDP> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body(forbiddenError.errorDP)
    }

    @ExceptionHandler(NotFoundError::class)
    fun handleNotFoundErrors(notFoundError: NotFoundError): ResponseEntity<ErrorDP> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(notFoundError.errorDP)
    }

    @ExceptionHandler(
        BadRequestError::class,
        ConstraintViolationException::class,
        MethodArgumentNotValidException::class,
        IllegalArgumentException::class
    )
    fun handleBadRequestErrors(error: Exception): ResponseEntity<ErrorsDP> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(
            when (error) {
                is BadRequestError -> error.errorsDP
                is ConstraintViolationException -> ErrorsDP(error.constraintViolations.stream().map { it.message }.toList())
                is MethodArgumentNotValidException -> ErrorsDP(error.bindingResult.fieldErrors.stream().map<String>{ it.defaultMessage }.toList())
                is IllegalArgumentException -> ErrorsDP(listOf(error.message ?: "Some validation failed."))
                else -> throw InternalServerError(ErrorDP("Unexpected error occurred."))
            }
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupportedErrors(error: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorDP> {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).contentType(MediaType.APPLICATION_JSON).body(ErrorDP("Method not supported on this endpoint."))
    }

    @ExceptionHandler(
        InternalServerError::class,
        Exception::class
    )
    fun handleOtherErrors(error: Exception): ResponseEntity<ErrorDP> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(
            when (error) {
                is InternalServerError -> error.errorDP
                else -> ErrorDP("Unexpected error occurred.")
            }
        )
    }
}