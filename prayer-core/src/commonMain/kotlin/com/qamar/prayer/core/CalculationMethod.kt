package com.qamar.prayer.core

/**
 * Calculation parameters describe how Fajr, Maghrib and Isha are derived.
 * Values are taken from the legacy QamarOld `PrayerTime` implementation.
 */
enum class ParameterType { ANGLE, MINUTES }

data class MethodParams(
    val fajrAngle: Double,
    val maghribType: ParameterType,
    val maghribParameter: Double,
    val ishaType: ParameterType,
    val ishaParameter: Double,
)

@Suppress("MagicNumber")
enum class CalculationMethod(internal val params: MethodParams) {
    QMDB(MethodParams(15.0, ParameterType.ANGLE, 0.0, ParameterType.ANGLE, 15.0)),
    KARACHI(MethodParams(18.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 18.0)),
    ISNA(MethodParams(15.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 15.0)),
    MWL(MethodParams(18.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 17.0)),
    MAKKAH(MethodParams(18.5, ParameterType.MINUTES, 0.0, ParameterType.MINUTES, 90.0)),
    EGYPT(MethodParams(19.5, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 17.5)),
    TEHRAN(MethodParams(17.7, ParameterType.ANGLE, 4.5, ParameterType.ANGLE, 14.0)),
    JAFARI(MethodParams(16.0, ParameterType.ANGLE, 4.0, ParameterType.ANGLE, 14.0)),
    DIYANET(MethodParams(18.0, ParameterType.ANGLE, 0.0, ParameterType.ANGLE, 17.0)),

    /** UAE — General Authority of Islamic Affairs & Endowments. */
    DUBAI(MethodParams(18.2, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 18.2)),

    /** Kuwait — Ministry of Awqaf and Islamic Affairs. */
    KUWAIT(MethodParams(18.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 17.5)),

    /** Qatar — Ministry of Awqaf and Islamic Affairs; fixed 90-minute Isha. */
    QATAR(MethodParams(18.0, ParameterType.MINUTES, 0.0, ParameterType.MINUTES, 90.0)),

    /** Singapore — Majlis Ugama Islam Singapura (MUIS). */
    SINGAPORE(MethodParams(20.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 18.0)),

    /** Malaysia — Jabatan Kemajuan Islam Malaysia (JAKIM). */
    JAKIM(MethodParams(20.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 18.0)),

    /** Indonesia — Kementerian Agama (Kemenag). */
    KEMENAG(MethodParams(20.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 18.0)),

    /** France — Union des Organisations Islamiques de France; 12°/12°. */
    UOIF(MethodParams(12.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 12.0)),

    /** Russia — Spiritual Administration of Muslims of Russia. */
    RUSSIA(MethodParams(16.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 15.0)),

    /** Morocco — Ministry of Habous and Islamic Affairs. */
    MOROCCO(MethodParams(19.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 17.0)),

    /** Algeria — Ministry of Religious Affairs and Wakfs. */
    ALGERIA(MethodParams(18.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 17.0)),

    /** Tunisia — Ministry of Religious Affairs. */
    TUNISIA(MethodParams(18.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 18.0)),

    CUSTOM(MethodParams(18.0, ParameterType.MINUTES, 0.0, ParameterType.ANGLE, 17.0)),
}

enum class JuristicMethod { SHAFII, HANAFI }

enum class HighLatitudeRule { NONE, MID_NIGHT, ONE_SEVENTH, ANGLE_BASED }

enum class TimeFormat { TIME_24, TIME_12, TIME_12_NO_SUFFIX, FLOATING }
