package com.example.template.item.application

import com.example.template.common.error.NotFoundException

class ItemNotFoundException(id: Long) : NotFoundException("item not found: $id")
