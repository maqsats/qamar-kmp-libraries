package com.qamar.prayer.core

/**
 * Android implementation using the device's default time zone (via kotlinx-datetime).
 * Same behavior as [DefaultTimeZoneProvider] on all platforms.
 */
class AndroidTimeZoneProvider : TimeZoneProvider by DefaultTimeZoneProvider()
