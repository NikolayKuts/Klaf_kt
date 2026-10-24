package com.kuts.klaf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.maintenance.TemporaryCleanupResult
import com.kuts.klaf.room.maintenance.TemporaryCleanupStartup
import com.kuts.klaf.room.maintenance.TemporaryCleanupStartupState
import com.kuts.klaf.room.maintenance.TemporaryVocabularyCleanup
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val temporaryCleanupGateMutex = Mutex()

// TEMPORARY maintenance build only. Remove this file and MainActivity hook after confirmation.
@Composable
internal fun TemporaryVocabularyCleanupGate(
    databaseSource: RoomDatabaseSource,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val startup = remember(databaseSource, context) {
        val receipts = TemporaryCleanupFileReceipts(File(context.filesDir, "temporary-vocabulary-cleanup-receipts"))
        val cleanup = TemporaryVocabularyCleanup(databaseSource, receipts)
        TemporaryCleanupStartup {
            temporaryCleanupGateMutex.withLock {
                cleanup.run { json ->
                    val directory = File(context.filesDir, "temporary-vocabulary-cleanup-backups")
                    check(directory.isDirectory || directory.mkdirs()) { "Не удалось создать папку резервной копии" }
                    val file = File.createTempFile("vocabulary-", ".json", directory)
                    FileOutputStream(file).use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                        output.fd.sync()
                    }
                }
            }
        }
    }
    var attempt by remember { mutableStateOf(0) }
    LaunchedEffect(startup, attempt) {
        withContext(Dispatchers.IO) { startup.run() }
    }
    val state by startup.state.collectAsState()
    if (state == TemporaryCleanupStartupState.Ready) {
        content()
        return
    }
    TemporaryCleanupReport(state, onOpenApp = startup::openApp, onRetry = { attempt++ })
}

@Composable
internal fun TemporaryCleanupReport(
    state: TemporaryCleanupStartupState,
    onOpenApp: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Временная сборка: разовая очистка Vocabulary Source")
            when (val current = state) {
                TemporaryCleanupStartupState.Running -> {
                    CircularProgressIndicator()
                    Text("Подготовка резервной копии и очистка старых результатов…")
                }
                is TemporaryCleanupStartupState.Report -> when (val result = current.result) {
                    is TemporaryCleanupResult.Completed -> {
                        Text("Локальная очистка завершена. Источник: ${result.receipt.sourceTitle}")
                        Text("Удалено айтемов: ${result.receipt.removedItems}. Ignored Words: ${result.receipt.removedIgnoredWords}.")
                        Text("Данные источника, тексты, Card и Deck сохранены. Создана резервная копия.")
                        if (result.receipt.syncRequired) {
                            Text("Открой приложение и выполни обычную синхронизацию для передачи удалений серверу.")
                        }
                        Text("После проверки сообщи об успехе, чтобы получить сборку без временного кода очистки.")
                        Button(onClick = onOpenApp) { Text("Открыть приложение") }
                    }
                    is TemporaryCleanupResult.Skipped -> {
                        Text("Очистка не выполнялась: в выбранной БД источников ${result.sourceCount}, ожидался один.")
                        Text("Данные не изменены. Закрой приложение и сообщи это число перед продолжением.")
                        Button(onClick = onRetry) { Text("Проверить ещё раз") }
                    }
                }
                is TemporaryCleanupStartupState.Failed -> {
                    Text("Очистка не завершена: ${current.message}")
                    Text("Рабочий экран пока закрыт. Результат будет сохранён перед продолжением.")
                    Button(onClick = onRetry) { Text("Повторить") }
                }
                TemporaryCleanupStartupState.Ready -> Unit
            }
        }
    }
}
