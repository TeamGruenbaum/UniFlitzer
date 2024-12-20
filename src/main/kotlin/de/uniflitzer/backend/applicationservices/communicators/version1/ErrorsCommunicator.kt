package de.uniflitzer.backend.applicationservices.communicators.version1

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.ErrorsDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.TraceableErrorDP
import de.uniflitzer.backend.applicationservices.communicators.version1.datapackages.TraceableErrorsDP
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.BadRequestError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.ForbiddenError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.InternalServerError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.NotFoundError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.StompError
import de.uniflitzer.backend.applicationservices.communicators.version1.errors.UnprocessableContentError
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.converter.MessageConversionException
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.simp.user.SimpUserRegistry
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.util.*
import kotlin.collections.toList

@ControllerAdvice
private class ErrorsCommunicator(
    @field:Autowired private val clientOutboundChannel: MessageChannel
) {
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
                is ConstraintViolationException -> ErrorsDP(error.toList())
                is MethodArgumentNotValidException -> ErrorsDP(error.toList())
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

    @ExceptionHandler(UnprocessableContentError::class)
    fun handleMethodNotSupportedErrors(error: UnprocessableContentError): ResponseEntity<ErrorDP> {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON).body(error.errorDP)
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

    @MessageExceptionHandler
    fun handleAllErrors(error: Exception, currentMessage: Message<*>, @Header("simpSessionId") currentSessionId: String) {
        fun closeSession(errorMessage: String) {
            clientOutboundChannel.send(
                MessageBuilder.createMessage(
                    ByteArray(5),
                    StompHeaderAccessor
                        .create(StompCommand.ERROR)
                        .apply {
                            message = errorMessage
                            sessionId = currentSessionId
                        }
                        .messageHeaders
                )
            )
        }

        val objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
        }

        closeSession(
            objectMapper.writeValueAsString(
                when (error) {
                    is StompError -> error.errorsDP
                    is ConstraintViolationException -> ErrorsDP(error.toList())
                    is MethodArgumentNotValidException -> ErrorsDP(error.toList())
                    is MethodArgumentTypeMismatchException -> ErrorsDP(listOf("Value for parameter \"${error.name}\" has wrong type."))
                    else -> {
                        val traceId: UUID = UUID.randomUUID()
                        logger.error(
                            """An unexpected error occurred. (Trace-ID: $traceId)
MESSAGE:
Headers: ${objectMapper.writeValueAsString(currentMessage.headers.toMap())}
Payload: ${objectMapper.writeValueAsString(currentMessage.payload)}

STACKTRACE:
${error.stackTraceToString()}
"""
                        )

                        TraceableErrorsDP(traceId.toString(), listOf<String>("An unexpected error occurred."))
                    }
                }
            )
        )
    }

    private fun ConstraintViolationException.toList():List<String> =
        this.constraintViolations.stream().map { "\"${it.propertyPath.toList().last().name}\": ${it.messageTemplate}" }.toList()

    private fun MethodArgumentNotValidException.toList():List<String> =
        this.bindingResult.fieldErrors.stream().map{ "${it.field}: ${it.defaultMessage}" }.toList()
}

