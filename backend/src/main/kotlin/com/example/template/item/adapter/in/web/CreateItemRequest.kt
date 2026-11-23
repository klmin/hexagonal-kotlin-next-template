package com.example.template.item.adapter.`in`.web

import com.example.template.item.application.port.`in`.CreateItemCommand
import com.example.template.item.domain.Item
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * name 을 String? 로 두는 이유: String(non-null)이면 name 이 빠진 요청을 Jackson 이 역직렬화 단계에서 실패시켜
 * Bean Validation 오류(errors[].field = "name")가 아니라 "JSON 형식 오류"가 된다. 자바 버전과 같은 응답을 주려고 nullable + @NotBlank.
 *
 * @field: 를 붙이는 이유: 코틀린 생성자 프로퍼티에 그냥 @NotBlank 를 붙이면 생성자 파라미터에 붙는다.
 * Bean Validation 은 필드(또는 getter)를 보므로 대상(field)을 지정해야 검증이 된다.
 *
 * springdoc 은 String? 을 보면 스펙에 "string 또는 null" 로 적는데, application.yml 에서 그 기능을 꺼서
 * 자바 버전과 같은 API 계약(프론트 타입)을 유지한다 (springdoc.model-converters.kotlin-nullable-property-customizer).
 */
data class CreateItemRequest(
    @field:NotBlank @field:Size(max = Item.NAME_MAX_LENGTH) val name: String?,
    @field:Size(max = Item.DESCRIPTION_MAX_LENGTH) val description: String? = null,
) {
    // internal: 같은 모듈 안에서만 보인다. 자바의 package-private 에 가장 가까운 것 (코틀린에는 패키지 단위 공개 범위가 없다)
    // !! : @Valid 를 통과했으니 name 은 null 이 아니다. null 이면 NullPointerException
    internal fun toCommand() = CreateItemCommand(name!!, description)
}
