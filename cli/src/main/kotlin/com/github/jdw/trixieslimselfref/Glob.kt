package com.github.jdw.trixieslimselfref

import com.github.jdw.trixieslimselfref.subcommands.general.DiscordWebhookMessage
import com.github.jdw.trixieslimselfref.subcommands.general.Settings
import doch
import echt
import fuel.httpPost
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object Glob { // The global object
	var verbose = false
	val baseDir = "${System.getenv("HOME")}/.trixie-slim-selfref"

	enum class ExitCodes { // Never change the order of these, never delete an entry, only add, always add at the bottom
		ALL_OK, // exit code 0
		CRON_NOTHING_TO_DO, // exit code 1
		CRON_NO_SETTINGS_DIR, // exit code 2
		GLOB_SETTINGS_FILE_NOT_FOUND, // exit code 3
		GLOB_SETTINGS_FILE_PARSE_FAIL, // exit code 4
		CRON_SETTINGS_NO_ARCHITECTURES_IN_SETTINGS_FILE, // exit code 5
		CRON_FAILED_GETTING_BRANCH_NAMES, // exit code 6
		CRON_SETTINGS_BAD_DATA_gitChecksumsDir, // exit code 7
		CRON_SETTINGS_BAD_DATA_gitRepoDir, // exit code 8
		CRON_SETTINGS_BAD_DATA_upstreamChecksumUrl, // exit code 9
		SETTINGS_BAD_DATA, // exit code 10
		CRON_SETTINGS_BAD_DATA_upstreamRepoBranchPrefix, // exit code 11
		COULD_NOT_GET_CHECKSUM_FOR_ARCHITECTURE, // exit code 12
		DIRECTORY_DOES_NOT_EXIST, // exit code 13
		DIRECTORY_DOES_NOT_EXIST_gitRepoDir, // exit code 13
		FAILED_GETTING_INFORMATION_ABOUT_BRANCHES, // exit code 14
		CRON_FAILED_COMMIT_AND_PUSH_CHECKSUMS, // exit code 15
		FAILED_ENTER_DIR, // exit code 16
		CRON_FAILED_GETTING_DIFF, // exit code 17
		CRON_FAILED_COMMIT_AND_PUSH, // exit code 17
		FAILED_SAVING_CHECKSUM, // exit code 18
		FAILED_PULLING_CHECKSUMS, // exit code 19
		GENERAL_NO_SETTINGS_FILE, // exit code 20
		GENERAL_SETTINGS_FILE_PARSE_FAIL, // exit code 21
		SETTINGS_NO_WEBHOOK_URL_IN_SETTINGS_FILE, // exit code 22
		GENERAL_SETTINGS_NO_WEBHOOK_USERNAME_IN_SETTINGS_FILE, // exit code 23
		GIT_COMMAND_FAILED // exit code 24
	}

	fun message(msg: String, error: Boolean = false) {
		error
			.echt {
				val totzMsg = "ERROR :: $msg"
				println(totzMsg)
				webhookMessage(totzMsg)
			}
			.doch { verbose.echt { println(msg) } }
	}

	fun exitProcess(errorMsg: String, exitCode: ExitCodes): Nothing {
		message(errorMsg, true)
		kotlin.system.exitProcess(exitCode.ordinal)
	}

	fun webhookMessage(message: String) {
		if (settingsCache == null) {
			println("ERROR :: Wanted to send '$message' to webhook but could not because 'settingsCache' was null!")
			return
		}

		val (settings, _) = settingsCache!!

		runBlocking {
			val response = settings
				.webhook
				.url
				.httpPost(body = Json.encodeToString(DiscordWebhookMessage(content = message, username = settings.webhook.username)), headers = mapOf("Content-Type" to "application/json"))


			(response.statusCode == 200)
				.doch {
					println(String(response.body.bytes()).trim())
				}
		}
	}

	private var settingsCache: Pair<Settings, String>? = null
	fun loadSettings(): Pair<Settings, String> {
		return settingsCache ?: run {
			val settingsFile = File("$baseDir/settings.json")
			settingsFile.isFile.doch {
				exitProcess(
					"File '${settingsFile.absoluteFile}' not found!",
					ExitCodes.GLOB_SETTINGS_FILE_NOT_FOUND
				)
			}

			try {
				settingsCache = Pair(Json.decodeFromString<Settings>(settingsFile.readText()), settingsFile.absolutePath)
			} catch (_: Exception) {
				exitProcess(
					"Failed to parse file '${settingsFile.absoluteFile}'!",
					ExitCodes.GLOB_SETTINGS_FILE_PARSE_FAIL
				)
			}

			settingsCache!!
		}
	}
}