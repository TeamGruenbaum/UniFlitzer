package de.uniflitzer.backend.applicationservices.communicators.version1

import com.fasterxml.jackson.databind.ObjectMapper
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorsDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.TraceableErrorDP
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.BadRequestError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.InternalServerError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.util.*

@ControllerAdvice
private class ErrorsCommunicator {
    private val logger = LoggerFactory.getLogger(ErrorsCommunicator::class.java)

    @ExceptionHandler(ForbiddenError::class)
    fun handleForbiddenErrors(forbiddenError: ForbiddenError): ResponseEntity<ErrorDP> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body(forbiddenError.errorDP)
    }

    @ExceptionHandler(
        NotFoundError::class,
        NoResourceFoundException::class
    )
    fun handleNotFoundErrors(exception: Exception): ResponseEntity<ErrorDP> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(
            when(exception) {
                is NotFoundError -> exception.errorDP
                is NoResourceFoundException -> ErrorDP("Resource not found.")
                else -> throw InternalServerError(ErrorDP("Unexpected error occurred."))
            }
        )
    }

    @ExceptionHandler(
        BadRequestError::class,
        ConstraintViolationException::class,
        MethodArgumentNotValidException::class,
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
        HttpMessageNotReadableException::class,
        IllegalArgumentException::class,
        MaxUploadSizeExceededException::class
    )
    fun handleBadRequestErrors(error: Exception): ResponseEntity<ErrorsDP> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(
            when (error) {
                is BadRequestError -> error.errorsDP
                is ConstraintViolationException -> ErrorsDP(error.constraintViolations.stream().map { "\"${it.propertyPath.toList().last().name}\": ${it.messageTemplate}" }.toList())
                is MethodArgumentNotValidException -> ErrorsDP(error.bindingResult.fieldErrors.stream().map{ "${it.field}: ${it.defaultMessage}" }.toList())
                is MethodArgumentTypeMismatchException -> ErrorsDP(listOf("Value for parameter \"${error.name}\" has wrong type."))
                is MissingServletRequestParameterException -> ErrorsDP(listOf("Value for parameter \"${error.parameterName}\" is missing."))
                is HttpMessageNotReadableException -> ErrorsDP(listOf("Request body is missing or has wrong format."))
                is IllegalArgumentException -> ErrorsDP(listOf("Some validation failed."))
                is MaxUploadSizeExceededException -> ErrorsDP(listOf("File size exceeds the limit of 5MB."))
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
    fun handleOtherErrors(error: Exception, request: HttpServletRequest): ResponseEntity<TraceableErrorDP> {
        val traceId: UUID = UUID.randomUUID()
        logger.error(
"""An unexpected error occurred. (Trace-ID: $traceId)
REQUEST:
Headers: ${ObjectMapper().writeValueAsString(request.headerNames.toList().associateWith{request.getHeader(it)})}
Method: ${request.method}
Path: ${request.requestURI}
QueryParams: ${request.queryString ?: "None"}
Body: ${try{ request.reader.readText() }catch(_: Exception){ request.contentType }}

STACKTRACE:
${error.stackTraceToString()}
"""
        )

        return ResponseEntity<TraceableErrorDP>(
            when (error) {
                is InternalServerError -> TraceableErrorDP(traceId.toString(), error.errorDP.message)
                else -> TraceableErrorDP(traceId.toString(), "Unexpected error occurred.")
            },
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}