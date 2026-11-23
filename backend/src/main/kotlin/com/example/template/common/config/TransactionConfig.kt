package com.example.template.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.RollbackOn

/**
 * 모든 예외에서 롤백한다.
 *
 * Spring 은 기본으로 RuntimeException/Error 에서만 롤백하고 checked 예외(IOException 등)는 커밋한다.
 * 자바는 컴파일러가 checked 예외를 드러내지만 코틀린은 선언 없이 그냥 던지므로,
 * 서비스에서 자바 API 가 IOException 을 던지면 일부만 저장된 채 커밋될 수 있다 (TransactionConfigTest).
 *
 * proxyTargetClass = true: Spring Boot 기본값(클래스 기반 프록시)을 유지한다.
 * 이 어노테이션을 직접 붙이면 Boot 의 트랜잭션 자동 설정이 물러나므로 같이 적는다.
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement(proxyTargetClass = true, rollbackOn = RollbackOn.ALL_EXCEPTIONS)
class TransactionConfig
