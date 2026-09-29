package com.kuts.domain.common

enum class ConflictResolutionAction {
    ACCEPT_SERVER,
    KEEP_LOCAL_DECK,
    KEEP_LOCAL_CARD,
    KEEP_LOCAL_SOURCE,
    RESCUE_MOVED_CARD,
    RESTORE_DELETED_DECK,
    KEEP_REMOVAL_RETAIN_SCHEDULE,
    KEEP_REMOVAL_DUE_NOW,
    RETARGET_MOVED_CARD,
}

data class ConflictResolutionDecision(
    val operationId: String,
    val action: ConflictResolutionAction,
)
