package com.sora.coredatabase.converter

import androidx.room.TypeConverter
import com.sora.coremodel.MatchStatus
import com.sora.coremodel.MediaType
import com.sora.coremodel.SourceType
import com.sora.coremodel.UnitType

/**
 * Enum <-> String converters.
 *
 * Enums are persisted by *name*, not ordinal. Ordinals are positional, so
 * inserting a new constant in the middle of an enum would silently reinterpret
 * every existing row - a data-corruption bug with no error message. Names cost
 * a few bytes and fail loudly instead.
 */
object SoraTypeConverters {

    @TypeConverter
    fun mediaTypeToString(value: MediaType): String = value.name

    @TypeConverter
    fun stringToMediaType(value: String): MediaType = MediaType.valueOf(value)

    @TypeConverter
    fun sourceTypeToString(value: SourceType): String = value.name

    @TypeConverter
    fun stringToSourceType(value: String): SourceType = SourceType.valueOf(value)

    @TypeConverter
    fun matchStatusToString(value: MatchStatus): String = value.name

    @TypeConverter
    fun stringToMatchStatus(value: String): MatchStatus = MatchStatus.valueOf(value)

    @TypeConverter
    fun unitTypeToString(value: UnitType): String = value.name

    @TypeConverter
    fun stringToUnitType(value: String): UnitType = UnitType.valueOf(value)
}
