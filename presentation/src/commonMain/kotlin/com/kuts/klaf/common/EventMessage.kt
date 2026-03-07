package com.kuts.klaf.common

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class EventMessage(
    val text: UiText,
    val type: Type = Type.Neutral,
    val duration: Duration = Duration.Medium,
) {
    constructor(
        resId: StringResource,
        vararg args: Any,
        type: Type = Type.Neutral,
        duration: Duration = Duration.Medium,
    ) : this(
        text = UiText.Resource(resource = resId, args = args.toList()),
        type = type,
        duration = duration,
    )

    constructor(
        value: String,
        type: Type = Type.Neutral,
        duration: Duration = Duration.Medium,
    ) : this(
        text = UiText.Plain(value = value),
        type = type,
        duration = duration,
    )

    enum class Type { Negative, Waring, Neutral, Positive }

    enum class Duration {

        Short,
        Medium,
        Long;

        val value: Long
            get() = when (this) {
                Short -> 2000L
                Medium -> 2600L
                Long -> 3200L
            }
    }
}

sealed interface UiText {
    data class Plain(val value: String) : UiText
    data class Resource(
        val resource: StringResource,
        val args: List<Any> = emptyList(),
    ) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> value
    is UiText.Resource -> stringResource(resource = resource, *args.toTypedArray())
}
