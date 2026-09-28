package com.kuts.klaf

import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kuts.domain.managers.GUEST_REVIEW_REMINDER_SCOPE
import com.kuts.domain.managers.IReviewReminderScopeProvider
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.klaf.common.AndroidScopedDeckReminderActions
import com.kuts.klaf.common.DeckReviewReceiver
import com.kuts.klaf.common.notifications.AndroidDeckReviewNotifier
import com.kuts.klaf.common.notifications.NotificationChannelInitializer
import com.kuts.klaf.navigation.AppLaunchNavigationExtras
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.AndroidSelectedAccountStore
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.LocalRoomDatabaseFactory
import com.kuts.klaf.room.databases.RoomReminderSelectionObserver
import com.kuts.klaf.room.databases.SelectedAccountStore
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.entities.RoomCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class ScopedDeckReminderInstrumentedTest {

    @get:Rule
    val isolatedApplication = IsolatedStorageTestRule()

    @Test
    fun scheduledAlarmPublishesNotificationForSelectedAccount() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        check(manager.areNotificationsEnabled()) { "Notifications are disabled for the isolated test app" }
        val accountStore = AndroidSelectedAccountStore(context)
        assumeTrue("This acceptance check needs account-scoped reminders", accountStore.areScopedRemindersActive())
        val account = accountStore.read()
        assumeTrue("This acceptance check needs a selected test account", account != null)
        val selectedAccount = requireNotNull(account)
        val deckId = 845_754
        val deckName = "Scheduled alarm test ${System.currentTimeMillis()}"
        val scheduledAt = System.currentTimeMillis() + 5_000L
        val reminders = AndroidScopedDeckReminderActions(context)

        try {
            reminders.dismissNotification(selectedAccount, deckId)
            awaitNotificationRemoval(manager, selectedAccount, deckId)
            reminders.schedule(selectedAccount, deckName, deckId, scheduledAt)
            assertNotNull(pendingIntent(context, selectedAccount, deckId))

            val deadline = SystemClock.elapsedRealtime() + 40_000L
            var delivered: android.service.notification.StatusBarNotification? = null
            while (SystemClock.elapsedRealtime() < deadline && delivered == null) {
                delivered = manager.activeNotifications.singleOrNull { it.tag == selectedAccount && it.id == deckId }
                if (delivered == null) Thread.sleep(250)
            }
            val notification = requireNotNull(delivered) {
                "Android did not deliver the account-scoped alarm notification within 40 seconds"
            }
            assertEquals(selectedAccount, notification.tag)
            assertEquals(deckId, notification.id)
            assertNotNull(notification.notification.contentIntent)
            check(notification.postTime >= scheduledAt - 1_000L) {
                "Notification appeared before the scheduled alarm time"
            }
        } finally {
            reminders.cancel(selectedAccount, deckId)
            reminders.dismissNotification(selectedAccount, deckId)
            navigationPendingIntent(context, selectedAccount, deckId)?.cancel()
        }
    }

    @Test
    fun notificationTapOpensSelectedAccountsDeckReviewScreen() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        check(manager.areNotificationsEnabled()) { "Notifications are disabled for the isolated test app" }
        val account = AndroidSelectedAccountStore(context).read()
        assumeTrue("This acceptance check needs a selected test account", account != null)
        val database = GlobalContext.get().get<ActiveLocalRoomDatabase>().current()
        val selectedAccount = requireNotNull(account)
        val deckName = "Notification tap test ${System.currentTimeMillis()}"
        val deck = scheduledDeck(deckName, System.currentTimeMillis()).copy(
            scheduledIterationDates = emptyList(),
            repetitionQuantity = 0,
            cardQuantity = 1,
        )
        val deckId = database.deckDao().insertNewDeck(deck).toInt()
        val notifier = GlobalContext.get().get<AndroidDeckReviewNotifier>()

        try {
            database.cardDao().insetCard(RoomCard(
                deckId = deckId,
                nativeWord = "test meaning",
                foreignWord = "test word",
                ipa = "",
                wordMeaningInsights = WordMeaningInsights.EMPTY,
                mnemonicJson = "{}",
            ))
            notifier.showIfCurrent(selectedAccount, deckName, deckId)
            awaitNotification(manager, selectedAccount, deckId)
            tapNotificationFromShade(deckName)
            instrumentation.waitForIdleSync()
            awaitRenderedReviewScreen(instrumentation.uiAutomation.rootInActiveWindow, deckName)
        } finally {
            instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            manager.cancel(selectedAccount, deckId)
            navigationPendingIntent(context, selectedAccount, deckId)?.cancel()
            database.deckDao().deleteDeck(deckId)
        }
    }

    @Test
    fun notificationPublisherKeepsAccountScopeAndNavigationIntentSeparate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        assumeTrue(manager.areNotificationsEnabled())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                NotificationChannelInitializer.DECK_REPETITION_CHANNEL_ID,
                "Deck repetition test",
                NotificationManager.IMPORTANCE_DEFAULT,
            ))
        }
        val deckId = 845_753
        val alice = "reminder-alice@example.test"
        val bob = "reminder-bob@example.test"
        var selectedScope = alice
        val notifier = AndroidDeckReviewNotifier(
            context = context,
            notificationManager = manager,
            reminderScope = object : IReviewReminderScopeProvider {
                override fun currentScope(): String = selectedScope
            },
        )

        try {
            notifier.showIfCurrent(bob, "Bob", deckId)
            assertNull(manager.activeNotifications.singleOrNull { it.tag == bob && it.id == deckId })

            notifier.showIfCurrent(alice, "Alice", deckId)
            val aliceNotification = awaitNotification(manager, alice, deckId)
            val aliceIntent = navigationPendingIntent(context, alice, deckId)
            assertNotNull(aliceIntent)
            assertEquals(aliceIntent, aliceNotification.notification.contentIntent)

            selectedScope = bob
            AndroidScopedDeckReminderActions(context).dismissNotification(alice, deckId)
            awaitNotificationRemoval(manager, alice, deckId)
            notifier.showIfCurrent(alice, "Alice stale", deckId)
            assertNull(manager.activeNotifications.singleOrNull { it.tag == alice && it.id == deckId })

            notifier.showIfCurrent(bob, "Bob", deckId)
            val bobNotification = awaitNotification(manager, bob, deckId)
            val bobIntent = navigationPendingIntent(context, bob, deckId)
            assertNotNull(bobIntent)
            assertEquals(bobIntent, bobNotification.notification.contentIntent)
            assertNotEquals(aliceIntent, bobIntent)
        } finally {
            manager.cancel(alice, deckId)
            manager.cancel(bob, deckId)
            navigationPendingIntent(context, alice, deckId)?.cancel()
            navigationPendingIntent(context, bob, deckId)?.cancel()
        }
    }

    @Test
    fun selectionObserverSchedulesOnlyCurrentAccountWithCollidingRoomDeckIds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val guest = inMemoryDatabase(context)
        val aliceDatabase = inMemoryDatabase(context)
        val bobDatabase = inMemoryDatabase(context)
        val alice = "reminder-alice@example.test"
        val bob = "reminder-bob@example.test"
        val atTime = System.currentTimeMillis() + 600_000L
        val guestId = guest.deckDao().insertNewDeck(scheduledDeck("Guest", atTime)).toInt()
        val aliceId = aliceDatabase.deckDao().insertNewDeck(scheduledDeck("Alice", atTime)).toInt()
        val bobId = bobDatabase.deckDao().insertNewDeck(scheduledDeck("Bob", atTime)).toInt()
        val reminders = AndroidScopedDeckReminderActions(context)
        val accountStore = object : SelectedAccountStore {
            private var email: String? = null

            override fun read(): String? = email

            override fun write(email: String?) {
                this.email = email
            }
        }
        val source = ActiveLocalRoomDatabase(
            factory = object : LocalRoomDatabaseFactory {
                override fun openGuest(): KlafRoomDatabase = guest

                override fun openAccount(email: String): KlafRoomDatabase = when (email) {
                    alice -> aliceDatabase
                    bob -> bobDatabase
                    else -> error("Unexpected test account: $email")
                }
            },
            accountStore = accountStore,
            selectionObserver = RoomReminderSelectionObserver(reminders),
        )

        try {
            assertEquals(guestId, aliceId)
            assertEquals(aliceId, bobId)
            reminders.schedule(null, "Guest", guestId, atTime)
            assertNotNull(pendingIntent(context, null, guestId))

            source.selectAccount(alice)
            assertNull(pendingIntent(context, null, guestId))
            assertNotNull(pendingIntent(context, alice, aliceId))

            source.selectAccount(bob)
            assertNull(pendingIntent(context, alice, aliceId))
            assertNotNull(pendingIntent(context, bob, bobId))

            source.selectAccount(alice)
            assertNull(pendingIntent(context, bob, bobId))
            assertNotNull(pendingIntent(context, alice, aliceId))
        } finally {
            reminders.cancel(null, guestId)
            reminders.cancel(alice, aliceId)
            reminders.cancel(bob, bobId)
            source.close()
        }
    }

    @Test
    fun sameLocalDeckIdUsesSeparateAlarmsForSeparateAccounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reminders = AndroidScopedDeckReminderActions(context)
        val deckId = 845_752
        val alice = "alice@example.test"
        val bob = "bob@example.test"
        val atTime = System.currentTimeMillis() + 600_000L

        try {
            reminders.schedule(alice, "Alice deck", deckId, atTime)
            reminders.schedule(bob, "Bob deck", deckId, atTime)

            val aliceAlarm = pendingIntent(context, alice, deckId)
            val bobAlarm = pendingIntent(context, bob, deckId)
            assertNotNull(aliceAlarm)
            assertNotNull(bobAlarm)
            assertNotEquals(aliceAlarm, bobAlarm)

            val legacyAlarm = PendingIntent.getBroadcast(
                context,
                deckId,
                Intent(context, DeckReviewReceiver::class.java).apply { action = "deck_review_scheduling" },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            assertNotNull(legacyAlarm)

            reminders.cancel(alice, deckId)
            assertNull(pendingIntent(context, alice, deckId))
            assertNotNull(pendingIntent(context, bob, deckId))
            assertNull(legacyPendingIntent(context, deckId))
        } finally {
            reminders.cancel(alice, deckId)
            reminders.cancel(bob, deckId)
        }
    }

    private fun inMemoryDatabase(context: Context): KlafRoomDatabase =
        Room.inMemoryDatabaseBuilder<KlafRoomDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private fun scheduledDeck(name: String, atTime: Long): RoomDeck = RoomDeck(
        name = name,
        creationDate = 1L,
        repetitionIterationDates = emptyList(),
        scheduledIterationDates = listOf(atTime),
        scheduledDateInterval = 0L,
        repetitionQuantity = 1,
        cardQuantity = 0,
        lastFirstRepetitionDuration = 0L,
        lastSecondRepetitionDuration = 0L,
        lastRepetitionIterationDuration = 0L,
        isLastIterationSucceeded = true,
    )

    private fun pendingIntent(context: Context, account: String?, deckId: Int): PendingIntent? {
        val intent = Intent(context, DeckReviewReceiver::class.java).apply {
            action = "deck_review_scheduling"
            data = Uri.parse("klaf://review/${Uri.encode(account ?: GUEST_REVIEW_REMINDER_SCOPE)}/$deckId")
        }
        return PendingIntent.getBroadcast(
            context,
            deckId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun navigationPendingIntent(context: Context, account: String, deckId: Int): PendingIntent? {
        val intent = Intent().apply {
            setClassName(context, "com.kuts.klaf.MainActivity")
            data = Uri.parse("klaf://review-navigation/${Uri.encode(account)}/$deckId")
            putExtra(AppLaunchNavigationExtras.REMINDER_ACCOUNT_SCOPE_KEY, account)
        }
        return PendingIntent.getActivity(
            context,
            deckId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun awaitNotification(
        manager: NotificationManager,
        account: String,
        deckId: Int,
    ): android.service.notification.StatusBarNotification {
        repeat(40) {
            manager.activeNotifications.singleOrNull { it.tag == account && it.id == deckId }
                ?.let { return it }
            Thread.sleep(50)
        }
        return manager.activeNotifications.single { it.tag == account && it.id == deckId }
    }

    private fun awaitNotificationRemoval(manager: NotificationManager, account: String, deckId: Int) {
        repeat(40) {
            if (manager.activeNotifications.none { it.tag == account && it.id == deckId }) return
            Thread.sleep(50)
        }
        assertNull(manager.activeNotifications.singleOrNull { it.tag == account && it.id == deckId })
    }

    private fun awaitRenderedReviewScreen(initialRoot: AccessibilityNodeInfo?, deckName: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var root = initialRoot
        repeat(50) {
            val labels = visibleText(root)
            val deckVisible = labels.any { deckName in it }
            val reviewStatsVisible = labels.any { it.startsWith("reviewed:") }
            if (deckVisible && reviewStatsVisible) return
            Thread.sleep(100)
            root = automation.rootInActiveWindow
        }
        throw AssertionError(
            "Notification did not open the rendered review screen for '$deckName'; " +
                "window=${root?.packageName}; visible=${visibleText(root)}",
        )
    }

    private fun tapNotificationFromShade(deckName: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        check(automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)) {
            "Android did not open the notification shade"
        }
        repeat(40) {
            val root = automation.rootInActiveWindow
            val notification = findNodeContainingText(root, deckName)
            if (notification != null) {
                var target: AccessibilityNodeInfo? = notification
                while (target != null && !target.isClickable) target = target.parent
                check(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                    "The notification for '$deckName' was not clickable"
                }
                return
            }
            Thread.sleep(100)
        }
        throw AssertionError("Notification not found in Android shade: '$deckName'")
    }

    private fun findNodeContainingText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.contains(text) == true || node.contentDescription?.contains(text) == true) return node
        repeat(node.childCount) { index ->
            findNodeContainingText(node.getChild(index), text)?.let { return it }
        }
        return null
    }

    private fun visibleText(node: AccessibilityNodeInfo?): List<String> {
        if (node == null) return emptyList()
        return buildList {
            node.text?.toString()?.let(::add)
            node.contentDescription?.toString()?.let(::add)
            repeat(node.childCount) { index -> addAll(visibleText(node.getChild(index))) }
        }.take(40)
    }

    private fun legacyPendingIntent(context: Context, deckId: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        deckId,
        Intent(context, DeckReviewReceiver::class.java).apply { action = "deck_review_scheduling" },
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )
}
