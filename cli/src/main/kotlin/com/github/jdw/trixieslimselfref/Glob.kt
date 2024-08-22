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

	enum class ExitCodes { // Never change the order of these, never delete an entry, only add, always add at the bottom
		ALL_OK, // exit code 0
		CRON_NOTHING_TO_DO, // exit code 1
		CRON_NO_SETTINGS_DIR, // exit code 2
		CRON_NO_SETTINGS_FILE, // exit code 3
		CRON_SETTINGS_FILE_PARSE_FAIL, // exit code 4
		CRON_SETTINGS_NO_ARCHITECTURES_IN_SETTINGS_FILE, // exit code 5
		CRON_FAILED_GETTING_BRANCH_NAMES, // exit code 6
		CRON_SETTINGS_BAD_DATA_gitChecksumsDir, // exit code 7
		CRON_SETTINGS_BAD_DATA_gitRepoDir, // exit code 8
		CRON_SETTINGS_BAD_DATA_upstreamChecksumUrl, // exit code 9
		CRON_SETTINGS_BAD_DATA_upstreamRepoApiUrl, // exit code 10
		CRON_SETTINGS_BAD_DATA_upstreamRepoBranchPrefix, // exit code 11
		Could_not_get_checksum_for_architecture, // exit code 12
		DIRECTORY_DOES_NOT_EXIST_gitChecksumsDir, // exit code 13
		DIRECTORY_DOES_NOT_EXIST_gitRepoDir, // exit code 13
		Could_not_get_information_about_branches, // exit code 14
		CRON_FAILED_COMMIT_AND_PUSH_CHECKSUMS, // exit code 15
		CRON_FAILED_ENTER_CHECKSUMS_BRANCH_DIR, // exit code 16
		CRON_FAILED_GETTING_DIFF, // exit code 17
		CRON_FAILED_COMMIT_AND_PUSH, // exit code 17
		CRON_FAILED_SAVING_CHECKSUM, // exit code 18
		CRON_FAILED_PULLING_CHECKSUMS, // exit code 19
		GENERAL_NO_SETTINGS_FILE, // exit code 20
		GENERAL_SETTINGS_FILE_PARSE_FAIL, // exit code 21
		GENERAL_SETTINGS_NO_WEBHOOK_URL_IN_SETTINGS_FILE, // exit code 22
		GENERAL_SETTINGS_NO_WEBHOOK_USERNAME_IN_SETTINGS_FILE // exit code 23
	}

	fun message(msg: String, error: Boolean = false) {
		error
			.echt {
				val totzMsg = "ERROR :: $msg"
				println(totzMsg)
				Glob.webhookMessage(totzMsg)
			}
			.doch { Glob.verbose.echt { println(msg) } }
	}

	fun exitProcess(errorMsg: String, exitCode: ExitCodes): Nothing {
		Glob.message(errorMsg, true)
		kotlin.system.exitProcess(exitCode.ordinal)
	}

	fun webhookMessage(message: String) {
		val settings = Json.decodeFromString<Settings>(File("${System.getenv("HOME")}/.trixie-slim-selfref/settings.json").readText())
		runBlocking {
			val response = settings
				.webhookUrl
				.httpPost(body = Json.encodeToString(DiscordWebhookMessage(content = message, username = settings.webhookUsername)), headers = mapOf("Content-Type" to "application/json"))


			(response.statusCode == 200)
				.doch {
					println(String(response.body.bytes()).trim())
				}
		}
	}
}