package com.bordrotakip.domain.model

enum class EmploymentType {
    STANDARD,
    RETIRED;

    companion object {
        fun fromStorageValue(value: String?): EmploymentType {
            return entries.firstOrNull { it.name == value } ?: STANDARD
        }
    }
}
