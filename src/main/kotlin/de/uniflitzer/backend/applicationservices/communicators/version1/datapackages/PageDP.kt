package de.uniflitzer.backend.applicationservices.communicators.version1.datapackages

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import kotlin.math.ceil

// In PageDP a list of objects with generic type are stored,
// if the class of stored objects has the JsonTypeInfo annotation
// the generation of type-Attributes during serialization does not work properly,
// because of that specific subtypes have to be defined.


abstract class PageDP<ContentType>(
    @field:Min(0) val maximumPage: Int,
    @field:Valid @field:Size(min = 0) @field:Max(50) val content: List<ContentType>
)


class PartialDriveOfferPageDP(maximumPage: Int, content: List<PartialDriveOfferDP>): PageDP<PartialDriveOfferDP>(maximumPage, content) {
    companion object {
        fun fromList(allItems:List<PartialDriveOfferDP>, page: UInt, perPage:UInt): PartialDriveOfferPageDP {
            val (maximumPage, content) = generatePage(allItems, page, perPage)
            return PartialDriveOfferPageDP(maximumPage, content)
        }
    }
}

class PartialDriveRequestPageDP(maximumPage: Int, content: List<PartialDriveRequestDP>): PageDP<PartialDriveRequestDP>(maximumPage, content) {
    companion object {
        fun fromList(allItems:List<PartialDriveRequestDP>, page: UInt, perPage:UInt): PartialDriveRequestPageDP {
            val (maximumPage, content) = generatePage(allItems, page, perPage)
            return PartialDriveRequestPageDP(maximumPage, content)
        }
    }
}

class PartialDrivePageDP(maximumPage: Int, content: List<PartialDriveDP>): PageDP<PartialDriveDP>(maximumPage, content) {
    companion object {
        fun fromList(allItems:List<PartialDriveDP>, page: UInt, perPage:UInt): PartialDrivePageDP {
            val (maximumPage, content) = generatePage(allItems, page, perPage)
            return PartialDrivePageDP(maximumPage, content)
        }
    }
}

class PartialCarpoolPageDP(maximumPage: Int, content: List<PartialCarpoolDP>): PageDP<PartialCarpoolDP>(maximumPage, content) {
    companion object {
        fun fromList(allItems:List<PartialCarpoolDP>, page: UInt, perPage:UInt): PartialCarpoolPageDP {
            val (maximumPage, content) = generatePage(allItems, page, perPage)
            return PartialCarpoolPageDP(maximumPage, content)
        }
    }
}

private fun <ContentType> generatePage(allItems:List<ContentType>, page: UInt, perPage:UInt): Pair<Int, List<ContentType>> {
    return Pair<Int, List<ContentType>> (
        ceil(allItems.size.toDouble() / perPage.toDouble()).toInt(),
        allItems.asSequence()
            .drop(perPage.toInt() * (page.toInt()-1))
            .take(perPage.toInt())
            .toList()
    )
}