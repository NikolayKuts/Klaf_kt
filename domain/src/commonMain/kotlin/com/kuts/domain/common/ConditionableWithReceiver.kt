package com.kuts.domain.common

sealed class ConditionableWithReceiver<T, V> : Conditionable<V>() {

    abstract val receiver: T?

    class ValuableCondition<T, V>(
        override val receiver: T,
        override val value: V,
    ) : ConditionableWithReceiver<T, V>()

    class ElseCondition<T, V> : ConditionableWithReceiver<T, V>() {

        override val receiver: T? = null
        override val value: V? = null
    }
}

inline infix fun <T, R> ConditionableWithReceiver<T, R>.otherwise(block: () -> R): R = when (this) {
    is ConditionableWithReceiver.ValuableCondition -> this.value
    is ConditionableWithReceiver.ElseCondition -> block()
}
