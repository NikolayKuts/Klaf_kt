package com.example.klaf.domain.useCases

import com.example.klaf.data.common.DataSynchronizationValidator
import com.example.klaf.data.common.StorageSaveVersionValidationData
import com.example.klaf.di.*
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject

class SynchronizeLocalAndRemoteDataUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val localDeckRepository: DeckRepository,
    @CardRepositoryRoomImp
    private val localCardRepository: CardRepository,
    @StorageSaveVersionRepositoryRoomImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    @DeckRepositoryFirestoreImp
    private val remoteDeckRepository: DeckRepository,
    @CardRepositoryFirestoreImp
    private val remoteCardRepository: CardRepository,
    @StorageSaveVersionRepositoryFirestoreImp
    private val remoteStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val dataSynchronizationValidator: DataSynchronizationValidator,
) {

    companion object {

        private const val INITIAL_STORAGE_SAVE_VERSION = 0L
    }

    operator fun invoke(): Flow<String> = channelFlow {
        manageDataSynchronization()
    }

    private suspend fun ProducerScope<String>.manageDataSynchronization() {
        withContext(Dispatchers.IO) {
            val localDecksDeferred = async { localDeckRepository.fetchAllDecks() }
            val localCarsDeferred = async { localCardRepository.fetchAllCards() }
            val localSaveVersionDeferred = async {
                localStorageSaveVersionRepository.fetchVersion()
            }

            val remoteDecksDeferred = async { remoteDeckRepository.fetchAllDecks() }
            val remoteCardsDeferred = async { remoteCardRepository.fetchAllCards() }
            val remoteSaveVersionDeferred = async {
                remoteStorageSaveVersionRepository.fetchVersion()
            }

            val localDecks = localDecksDeferred.await()
            val localCards = localCarsDeferred.await()
            val localSaveVersion = localSaveVersionDeferred.await()

            val remoteDecks = remoteDecksDeferred.await()
            val remoteCards = remoteCardsDeferred.await()
            val remoteSaveVersion = remoteSaveVersionDeferred.await()

            val storageSaveVersionValidationData = StorageSaveVersionValidationData(
                localStorageSaveVersion = localSaveVersion,
                remoteStorageSaveVersion = remoteSaveVersion,
            )

            when {
                dataSynchronizationValidator.areSaveVersionUndefined(
                    validationData = storageSaveVersionValidationData
                ) -> {
                    localStorageSaveVersionRepository.insertVersion(
                        version = INITIAL_STORAGE_SAVE_VERSION
                    )
                    remoteStorageSaveVersionRepository.insertVersion(
                        version = INITIAL_STORAGE_SAVE_VERSION
                    )
                }

                dataSynchronizationValidator.shouldLocalStorageBeUpdated(
                    validationData = storageSaveVersionValidationData
                ) -> {
                    synchronizeData(
                        dataChannelFlow = this@manageDataSynchronization,
                        newerVersionDecks = remoteDecks,
                        newerVersionCards = remoteCards,
                        olderSaveVersionDecks = localDecks,
                        olderSaveVersionCards = localCards,
                        olderSaveVersionDeckRepository = localDeckRepository,
                        olderSaveVersionCardRepository = localCardRepository,
                        olderStorageSaveVersion = localSaveVersion
                    )
                }

                dataSynchronizationValidator.shouldRemoteStorageBeUpdated(
                    validationData = storageSaveVersionValidationData
                ) -> {
                    synchronizeData(
                        dataChannelFlow = this@manageDataSynchronization,
                        newerVersionDecks = localDecks,
                        newerVersionCards = localCards,
                        olderSaveVersionDecks = remoteDecks,
                        olderSaveVersionCards = remoteCards,
                        olderSaveVersionDeckRepository = remoteDeckRepository,
                        olderSaveVersionCardRepository = remoteCardRepository,
                        olderStorageSaveVersion = remoteSaveVersion
                    )
                }
            }
        }
    }

    private suspend fun synchronizeData(
        dataChannelFlow: ProducerScope<String>,
        newerVersionDecks: List<Deck>,
        newerVersionCards: List<Card>,
        olderSaveVersionDecks: List<Deck>,
        olderSaveVersionCards: List<Card>,
        olderSaveVersionDeckRepository: DeckRepository,
        olderSaveVersionCardRepository: CardRepository,
        olderStorageSaveVersion: Long?,
    ) {
        val synchronizationJobs = mutableListOf<Job>()
        val notContainedDecks = olderSaveVersionDecks.filter { deck ->
            !newerVersionDecks.contains(deck)
        }

        dataChannelFlow.launchAndAddJob(to = synchronizationJobs) {
            dataChannelFlow.deleteUnnecessaryDecks(
                notContainedDecks = notContainedDecks,
                olderSaveVersionDeckRepository = olderSaveVersionDeckRepository,
                synchronizationJobs = synchronizationJobs
            )
        }
        dataChannelFlow.launchAndAddJob(to = synchronizationJobs) {
            dataChannelFlow.insetDecks(
                newerVersionDecks = newerVersionDecks,
                olderSaveVersionDeckRepository = olderSaveVersionDeckRepository,
                synchronizationJobs = synchronizationJobs
            )
        }
        dataChannelFlow.launchAndAddJob(to = synchronizationJobs) {
            dataChannelFlow.deleteUnnecessaryCards(
                olderVersionCards = olderSaveVersionCards,
                newerVersionCards = newerVersionCards,
                notContainedDecks = notContainedDecks,
                olderSaveVersionCardRepository = olderSaveVersionCardRepository,
                synchronizationJobs = synchronizationJobs,
            )
        }

        dataChannelFlow.launchAndAddJob(to = synchronizationJobs) {
            dataChannelFlow.insertCards(
                newerVersionCards = newerVersionCards,
                olderSaveVersionCardRepository = olderSaveVersionCardRepository,
                synchronizationJobs = synchronizationJobs
            )
        }

        dataChannelFlow.launchAndAddJob(to = synchronizationJobs) {
            dataChannelFlow.updateStorageSaveVersion(
                oldVersion = olderStorageSaveVersion,
                synchronizationJobs = synchronizationJobs
            )
        }

        joinAll(jobs = synchronizationJobs.toTypedArray())
    }

    private fun ProducerScope<String>.deleteUnnecessaryDecks(
        notContainedDecks: List<Deck>,
        olderSaveVersionDeckRepository: DeckRepository,
        synchronizationJobs: MutableList<Job>,
    ) {
        notContainedDecks.onEach { deck ->
            launchAndAddJob(to = synchronizationJobs) {
                olderSaveVersionDeckRepository.removeDeck(deckId = deck.id)
                send(deck.name)
            }
        }
    }

    private fun ProducerScope<String>.insetDecks(
        newerVersionDecks: List<Deck>,
        olderSaveVersionDeckRepository: DeckRepository,
        synchronizationJobs: MutableList<Job>,
    ) {
        newerVersionDecks.onEach { deck ->
            launchAndAddJob(to = synchronizationJobs) {
                olderSaveVersionDeckRepository.insertDeck(deck = deck)
                send(deck.name)
            }
        }
    }

    private fun ProducerScope<String>.launchAndAddJob(
        to: MutableCollection<Job>,
        block: suspend ProducerScope<String>.() -> Unit,
    ) {
        launch { block() }
            .let { job -> to.add(job) }
    }

    private fun ProducerScope<String>.deleteUnnecessaryCards(
        olderVersionCards: List<Card>,
        newerVersionCards: List<Card>,
        notContainedDecks: List<Deck>,
        synchronizationJobs: MutableList<Job>,
        olderSaveVersionCardRepository: CardRepository,
    ) {
        val notContainedDeckIds = notContainedDecks.map { it.id }

        olderVersionCards.filter { card ->
            !newerVersionCards.contains(card) || notContainedDeckIds.contains(card.deckId)
        }.onEach { card ->
            launchAndAddJob(to = synchronizationJobs) {
                olderSaveVersionCardRepository.deleteCard(cardId = card.id)
                send(card.nativeWord)
            }
        }
    }

    private fun ProducerScope<String>.insertCards(
        newerVersionCards: List<Card>,
        olderSaveVersionCardRepository: CardRepository,
        synchronizationJobs: MutableList<Job>,
    ) {
        newerVersionCards.onEach { card ->
            launchAndAddJob(to = synchronizationJobs) {
                olderSaveVersionCardRepository.insertCard(card = card)
                send(card.nativeWord)
            }
        }
    }

    private fun CoroutineScope.updateStorageSaveVersion(
        oldVersion: Long?,
        synchronizationJobs: MutableList<Job>,
    ) {
        val updatedVersion =
            (oldVersion ?: StorageSaveVersionValidationData.UNDEFINED_SAVE_VERSION) + 1

        launch { remoteStorageSaveVersionRepository.insertVersion(version = updatedVersion) }
            .let { job -> synchronizationJobs.add(job) }
        launch { localStorageSaveVersionRepository.insertVersion(version = updatedVersion) }
            .let { job -> synchronizationJobs.add(job) }
    }
}