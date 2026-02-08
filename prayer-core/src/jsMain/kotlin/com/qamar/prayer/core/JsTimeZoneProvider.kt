package com.qamar.prayer.core

/**
 * JS implementation using the runtime's local time zone (via kotlinx-datetime).
 */
class JsTimeZoneProvider : TimeZoneProvider by DefaultTimeZoneProvider()
