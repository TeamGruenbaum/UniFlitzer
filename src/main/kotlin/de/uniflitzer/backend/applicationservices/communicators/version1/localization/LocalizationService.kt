package de.uniflitzer.backend.applicationservices.communicators.version1.localization

import jakarta.servlet.ServletException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.*


@Configuration
class LocalizationService: WebMvcConfigurer
{
    @Bean
    protected fun localeResolver(): LocaleResolver {
        return AcceptHeaderLocaleResolver().apply {
            setDefaultLocale(Locale.US)
        }
    }

    @Bean
    protected fun messageSource(): ReloadableResourceBundleMessageSource {
        return ReloadableResourceBundleMessageSource().apply {
            setBasename("classpath:messages")
            setDefaultEncoding("UTF-8")
        }
    }

    fun getMessage(key: String, vararg args: Any): String {
        val currentRequest = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?)?.request ?: throw ServletException("No current request found.")
        val locale:Locale = localeResolver().resolveLocale(currentRequest)
        return messageSource().getMessage(key, args, locale)
    }
}