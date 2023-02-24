package com.example.domain.useCases

import com.example.domain.common.*
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.entities.StorageSaveVersion
import com.example.domain.entities.StorageSaveVersion.Companion.INITIAL_SAVE_VERSION
import com.example.domain.repositories.CardRepository
import com.example.domain.repositories.DeckRepository
import com.example.domain.repositories.StorageSaveVersionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject

class SynchronizeLocalAndRemoteDataUseCase @Inject constructor(
    @LocalDeckRepositoryImp
    private val localDeckRepository: DeckRepository,
    @LocalCardRepositoryImp
    private val localCardRepository: CardRepository,
    @LocalStorageSaveVersionRepositoryImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    @RemoteDeckRepositoryImp
    private val remoteDeckRepository: DeckRepository,
    @RemoteCardRepositoryImp
    private val remoteCardRepository: CardRepository,
    @RemoteStorageSaveVersionRepositoryImp
    private val remoteStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val dataSynchronizationValidator: DataSynchronizationValidator,
) {

    companion object {

        private const val INCREMENT_STEP = 1
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
                localStorageSaveVersion = localSaveVersion?.version,
                remoteStorageSaveVersion = remoteSaveVersion?.version,
            )

            when {
                dataSynchronizationValidator.areSaveVersionUndefined(
                    validationData = storageSaveVersionValidationData
                ) -> {
                    localStorageSaveVersionRepository.insertVersion(
                        version = StorageSaveVersion()
                    )
                    remoteStorageSaveVersionRepository.insertVersion(
                        version = StorageSaveVersion()
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
                        targetStorageSaveVersion = remoteSaveVersion?.version
                    )
                }

                dataSynchronizationValidator.shouldRemoteStorageBeUpdated(
                    validationData = storageSaveVersionValidationData
                ) -> {
                    val targetVersion =
                        (remoteSaveVersion?.version ?: INITIAL_SAVE_VERSION) + INCREMENT_STEP

                    synchronizeData(
                        dataChannelFlow = this@manageDataSynchronization,
                        newerVersionDecks = localDecks,
                        newerVersionCards = localCards,
                        olderSaveVersionDecks = remoteDecks,
                        olderSaveVersionCards = remoteCards,
                        olderSaveVersionDeckRepository = remoteDeckRepository,
                        olderSaveVersionCardRepository = remoteCardRepository,
                        targetStorageSaveVersion = targetVersion
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
        targetStorageSaveVersion: Long?,
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
                targetVersion = targetStorageSaveVersion,
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
        targetVersion: Long?,
        synchronizationJobs: MutableList<Job>,
    ) {
        val updatedStorageSaveVersion =
            StorageSaveVersion(version = targetVersion ?: INITIAL_SAVE_VERSION)

        launch {
            remoteStorageSaveVersionRepository.insertVersion(version = updatedStorageSaveVersion)
        }.let { job -> synchronizationJobs.add(job) }

        launch {
            localStorageSaveVersionRepository.insertVersion(version = updatedStorageSaveVersion)
        }.let { job -> synchronizationJobs.add(job) }
    }
}