package com.kuts.domain.managers

enum class ImageSyncFailure { ID_REUSED, INVALID_FILE }

class ImageSynchronizationException(val failure: ImageSyncFailure) : IllegalStateException(failure.name)
