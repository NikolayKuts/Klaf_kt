package com.example.domain.common

sealed class ConditionableWithReversedReceiver<T, V> : Conditionable<V>(){

    abstract val receiver: T?

    class ValuableCondition<T, V>(
        override val value: V,
    ) : ConditionableWithReversedReceiver<T, V>() {

        override val receiver: T? = null
    }

    class ElseCondition<T, V>(
        override val receiver: T,
    ) : ConditionableWithReversedReceiver<T, V>() {

        override val value: V? = null
    }
}

inline infix fun <T, R> ConditionableWithReversedReceiver<T, R>.otherwise(
    block: (T) -> R,
): R = when (this) {
    is ConditionableWithReversedReceiver.ValuableCondition -> this.value
    is ConditionableWithReversedReceiver.ElseCondition -> block(this.receiver)
}