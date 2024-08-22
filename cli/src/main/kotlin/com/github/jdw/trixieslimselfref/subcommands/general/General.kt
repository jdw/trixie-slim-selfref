package com.github.jdw.trixieslimselfref.subcommands.general

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.jdw.trixieslimselfref.Glob
import doch
import echt
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

class General: CliktCommand(help="\uD83E\uDEE1 General subcommand") {
	private val checkSettings by option(help="Checks correctness of settings relevant for the general subcommand").flag()
	private val echoSettingsSkeleton by option(help = "Pretty prints a default settings file for the general subcommand and exits.").flag()

	override fun run() {
		checkSettings.echt { checkSettings() }
		echoSettingsSkeleton.echt { echoSettingsSkeleton() }
	}

	companion object {
		private fun loadSettings(filename: String): Settings {
			val settingsFile = File(filename)
			settingsFile.isFile.doch { Glob.exitProcess("File '${settingsFile.absoluteFile}' not found!", Glob.ExitCodes.GENERAL_NO_SETTINGS_FILE) }

			return try { Json.decodeFromString<Settings>(settingsFile.readText()) }
				catch (_: Exception) { Glob.exitProcess("Failed to parse file '${settingsFile.absoluteFile}'!", Glob.ExitCodes.GENERAL_SETTINGS_FILE_PARSE_FAIL) }
		}

		private fun checkSettings() {
			val settingsFilename = "${System.getenv("HOME")}/settings.json"
			val settings = loadSettings(settingsFilename)
			settings.webhookUrl.isEmpty().echt { Glob.exitProcess("No webhook URL in settings file ('$settingsFilename')!", Glob.ExitCodes.GENERAL_SETTINGS_NO_WEBHOOK_URL_IN_SETTINGS_FILE) }
			settings.webhookUsername.isEmpty().echt { Glob.exitProcess("No webhook username in settings file ('$settingsFilename')!", Glob.ExitCodes.GENERAL_SETTINGS_NO_WEBHOOK_USERNAME_IN_SETTINGS_FILE) }
		}

		private fun echoSettingsSkeleton() {
			val settings = Settings()
			val json = Json {
				prettyPrint = true
				encodeDefaults = true
			}

			println(json.encodeToString(settings))
			exitProcess(Glob.ExitCodes.ALL_OK.ordinal)
		}
	}
}