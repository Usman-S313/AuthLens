package com.authlens.app.domain.model

/**
 * The kind of document being verified. Each type can map to its own set of reference
 * templates for layout matching.
 */
enum class DocumentType(val displayName: String) {
    NATIONAL_ID("National ID"),
    PASSPORT("Passport"),
    DRIVERS_LICENSE("Driver's License"),
    BANK_STATEMENT("Bank Statement"),
    CHECK("Check"),
    INVOICE("Invoice"),
    GENERIC("Generic Document");

    companion object {
        /** Types that ship with built-in reference templates. */
        val templated: Set<DocumentType> get() = setOf(NATIONAL_ID, PASSPORT, GENERIC)
    }
}
