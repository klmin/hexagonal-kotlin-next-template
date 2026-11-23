package com.example.template.common.web

import com.example.template.common.error.DomainValidationException
import com.example.template.common.error.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.validation.method.ParameterErrors
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

// 오류 응답은 RFC 9457 Problem Details 형식으로 통일
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(DomainValidationException::class)
    fun handleDomainValidation(ex: DomainValidationException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message)

    // 처리하지 못한 예외도 Problem Details 로 응답한다. 내부 메시지는 응답에 넣지 않고 로그로만 남긴다
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ProblemDetail {
        log.error("unhandled exception", ex)
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.")
    }

    // @Valid @RequestBody 검증 실패
    // 부모 메서드의 반환값이 @Nullable 이라 코틀린에서는 ResponseEntity<Any>? 로 받는다 (-Xjsr305=strict)
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = ex.bindingResult.fieldErrors.map { FieldError(it.field, it.defaultMessage) }
        return handleExceptionInternal(ex, invalidInput(ex.body, errors), headers, status, request)
    }

    // @RequestParam, @PathVariable 등에 붙인 제약(@Min, @Max ...) 검증 실패.
    // 같은 메서드에 이런 제약이 있으면 @RequestBody 검증 실패도 이쪽으로 온다
    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = ex.parameterValidationResults.flatMap { it.toFieldErrors() }
        return handleExceptionInternal(ex, invalidInput(ex.body, errors), headers, status, request)
    }

    private fun invalidInput(body: ProblemDetail, errors: List<FieldError>): ProblemDetail = body.apply {
        detail = INVALID_INPUT
        setProperty("errors", errors)
    }

    // 확장 함수: ParameterValidationResult 에 메서드를 추가한 것처럼 쓴다. 이 파일 안에서만 보인다(private)
    private fun ParameterValidationResult.toFieldErrors(): List<FieldError> =
        if (this is ParameterErrors) {
            fieldErrors.map { FieldError(it.field, it.defaultMessage) }
        } else {
            val name = methodParameter.parameterName
            resolvableErrors.map { FieldError(name, it.defaultMessage) }
        }

    data class FieldError(val field: String?, val message: String?)

    // 자바의 private static final 필드 자리. 클래스마다 하나만 있는 값은 companion object 에 둔다
    private companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
        private const val INVALID_INPUT = "입력값을 확인해 주세요."
    }
}
