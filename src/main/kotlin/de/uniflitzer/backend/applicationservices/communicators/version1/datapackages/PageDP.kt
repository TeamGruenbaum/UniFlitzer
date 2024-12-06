package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import kotlin.math.ceil

data class PageDP<ContentType>(
    @field:Min(0) val maximumPage: Int,
    @field:Valid @field:Size(min = 0) val content: List<ContentType>
) {
    companion object {
        fun <ContentType> fromList(allItems:List<ContentType>, page: UInt, perPage:UInt): PageDP<ContentType> {
            return PageDP(
                maximumPage = ceil(allItems.size.toDouble() / perPage.toDouble()).toInt(),
                content = allItems.asSequence()
                    .drop(perPage.toInt() * (page.toInt()-1))
                    .take(perPage.toInt())
                    .toList()
            )
        }
    }
}