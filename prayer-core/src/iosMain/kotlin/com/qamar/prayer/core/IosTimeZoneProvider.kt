package com.qamar.prayer.core

/**
 * iOS implementation using the device's default time zone (via kotlinx-datetime).
 */
class IosTimeZoneProvider : TimeZoneProvider by DefaultTimeZoneProvider()
