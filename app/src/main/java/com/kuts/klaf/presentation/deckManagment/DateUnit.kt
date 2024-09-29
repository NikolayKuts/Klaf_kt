package com.kuts.klaf.presentation.deckManagment

sealed class DateUnit {

    abstract val value: Int

    companion object {

        private const val NON = -1


    }
    inline fun <reified T: DateUnit> Empty(): T = when (this) {
        is Day -> Day.Empty() as T as T
        is Hour -> Hour.Empty() as T
        is Minute -> Minute.Empty() as T
        is Month -> Month.Empty() as T
        is Week -> Week.Empty() as T
        is Year -> Year.Empty() as T
    } //Year(value = NON)

    interface Non {

        operator fun invoke(): DateUnit
    }

    fun isEmpty(): Boolean = this.value == NON

    data class Year(override val value: Int) : DateUnit(), Non by Empty {

        companion object Empty : Non {

            override fun invoke(): Year = Year(value = NON)
        }
    }

    data class Month(override val value: Int) : DateUnit(), Non by Empty {

        companion object Empty : Non {

            override operator fun invoke(): Month = Month(value = NON)
        }
    }

    data class Week(override val value: Int) : DateUnit(), Non by Empty {

        companion object Empty : Non {

            override operator fun invoke(): Week = Week(value = NON)
        }
    }

    data class Day(override val value: Int) : DateUnit(), Non by Empty {

        companion object Empty : Non {

            override operator fun invoke(): Day = Day(value = NON)
        }
    }

    data class Hour(override val value: Int) : DateUnit(), Non by Empty {

        companion object Empty : Non {

            override operator fun invoke(): Hour = Hour(value = NON)
        }
    }

    data class Minute(override val value: Int) : DateUnit() { //, Non by Empty {

        companion object Empty : Non {

            override operator fun invoke(): Minute = Minute(value = NON)
        }
    }
}