package com.example.template.item.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository

internal interface ItemJpaRepository : JpaRepository<ItemJpaEntity, Long>
