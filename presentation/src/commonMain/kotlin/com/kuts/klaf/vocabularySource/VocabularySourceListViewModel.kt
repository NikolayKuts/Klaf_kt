package com.kuts.klaf.vocabularySource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.useCases.CreateVocabularySourceUseCase
import com.kuts.domain.useCases.ObserveAllVocabularySourceItemsUseCase
import com.kuts.domain.useCases.ObserveVocabularySourcesUseCase
import com.kuts.domain.useCases.RemoveVocabularySourceUseCase
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.IEventMessageSource
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.problem_with_creating_vocabulary_source
import com.kuts.klaf.presentation.resources.problem_with_removing_vocabulary_source
import com.kuts.klaf.presentation.resources.vocabulary_source_created
import com.kuts.klaf.presentation.resources.vocabulary_source_removed
import com.kuts.klaf.presentation.resources.vocabulary_source_title_empty
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VocabularySourceListItemHolder(
    val source: VocabularySource,
    val pendingCount: Int = 0,
    val addedCount: Int = 0,
    val ignoredCount: Int = 0,
)

class VocabularySourceListViewModel(
    observeVocabularySources: ObserveVocabularySourcesUseCase,
    observeAllVocabularySourceItems: ObserveAllVocabularySourceItemsUseCase,
    private val createVocabularySource: CreateVocabularySourceUseCase,
    private val removeVocabularySource: RemoveVocabularySourceUseCase,
) : ViewModel(), IEventMessageSource {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    val sourceHolders: StateFlow<List<VocabularySourceListItemHolder>> = combine(
        observeVocabularySources(),
        observeAllVocabularySourceItems(),
    ) { sources, items ->
        val itemsBySourceId = items.groupBy { item -> item.sourceId }

        sources.map { source ->
            val sourceItems = itemsBySourceId[source.id].orEmpty()

            VocabularySourceListItemHolder(
                source = source,
                pendingCount = sourceItems.count { item -> item.status == VocabularySourceItemStatus.PENDING },
                addedCount = sourceItems.count { item -> item.status == VocabularySourceItemStatus.ADDED },
                ignoredCount = sourceItems.count { item -> item.status == VocabularySourceItemStatus.IGNORED },
            )
        }
    }
        .catch { emit(value = emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    fun createSource(
        title: String,
        description: String,
        onCreated: (Int) -> Unit,
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            eventMessage.tryEmit(
                EventMessage(
                    resId = Res.string.vocabulary_source_title_empty,
                    type = EventMessage.Type.Negative,
                )
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                val currentTime = getCurrentDateAsLong()
                createVocabularySource(
                    VocabularySource(
                        title = trimmedTitle,
                        description = description.trim(),
                        createdAt = currentTime,
                        updatedAt = currentTime,
                    )
                )
            }.onSuccess { sourceId ->
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_created,
                        type = EventMessage.Type.Positive,
                    )
                )
                onCreated(sourceId)
            }.onFailure {
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.problem_with_creating_vocabulary_source,
                        type = EventMessage.Type.Negative,
                    )
                )
            }
        }
    }

    fun deleteSource(sourceId: Int) {
        viewModelScope.launch {
            runCatching { removeVocabularySource(sourceId = sourceId) }
                .onSuccess {
                    eventMessage.tryEmit(
                        EventMessage(
                            resId = Res.string.vocabulary_source_removed,
                            type = EventMessage.Type.Positive,
                        )
                    )
                }
                .onFailure {
                    eventMessage.tryEmit(
                        EventMessage(
                            resId = Res.string.problem_with_removing_vocabulary_source,
                            type = EventMessage.Type.Negative,
                        )
                    )
                }
        }
    }
}
