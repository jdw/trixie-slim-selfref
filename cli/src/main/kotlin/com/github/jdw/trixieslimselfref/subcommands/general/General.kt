package com.github.jdw.trixieslimselfref.subcommands.general

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.jdw.trixieslimselfref.Glob
import com.github.jdw.trixieslimselfref.github.Branch
import doch
import echt
import fuel.httpGet
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

class General: CliktCommand(help="\uD83E\uDEE1 General subcommand", printHelpOnEmptyArgs = true) {
	private val checkSettings by option(help="Checks correctness of settings relevant for the general subcommand").flag()
	private val skeleton by option(help = "Pretty prints a default settings file for the general subcommand and exits.").flag()

	override fun run() {
		checkSettings.echt { checkSettings() }
		skeleton.echt { echoSettingsSkeleton() }
	}

	companion object {
		private fun checkSettings() {
			val filename = "${Glob.baseDir}/settings.json"
			Glob.settings.webhook.url.isEmpty()
				.echt { Glob.exitProcess("No webhook URL in '$filename'!", Glob.ExitCodes.SETTINGS_BAD_DATA) }
				.doch { Glob.message("Webhook URL OK") }
			Glob.settings.webhook.username.isEmpty()
				.echt { Glob.exitProcess("No webhook username in settings file ('$filename')!", Glob.ExitCodes.SETTINGS_BAD_DATA) }
				.doch { Glob.message("Webhook username OK") }

			// Checking existence of settings
			Glob.settings.architectures.isEmpty()
				.echt { Glob.exitProcess("No architectures in '$filename'!", Glob.ExitCodes.SETTINGS_BAD_DATA) }
				.doch { Glob.message("Architectures OK") }
			Glob.settings.git.branches.checksumsDir.isEmpty()
				.echt { Glob.exitProcess("No data in 'git.branches.checksumsDir' in '$filename'!", Glob.ExitCodes.SETTINGS_BAD_DATA) }
				.doch { Glob.message("Setting 'git.branches.checksumsDir' OK") }
			Glob.settings.git.branches.mainDir.isEmpty()
				.echt { Glob.exitProcess("No data in 'git.branches.mainDir' in '$filename'!", Glob.ExitCodes.SETTINGS_BAD_DATA) }
				.doch { Glob.message("Setting 'git.branches.mainDir' OK") }
			Glob.settings.upstream.checksumUrl.isEmpty()
				.echt { Glob.exitProcess("No data in 'upstream.checksumUrl' in '$filename'!", Glob.ExitCodes.SETTINGS_BAD_DATA) }
				.doch { Glob.message("Setting 'upstream.checksumUrl' OK") }
			Glob.settings.upstream.repoApiUrl.isEmpty()
				.echt { Glob.exitProcess("No data in 'upstream.repoApiUrl' in '$filename'!", Glob.ExitCodes.SETTINGS_BAD_DATA) }
				.doch { Glob.message("Setting 'upstream.repoApiUrl' OK") }
			Glob.settings.upstream.repoBranchPrefix.isEmpty()
				.echt { Glob.exitProcess("No data in 'upstream.repoBranchPrefix' in '$filename'!", Glob.ExitCodes.SETTINGS_BAD_DATA) }
				.doch { Glob.message("Setting 'upstream.repoBranchPrefix' OK") }

			// Checking validity of settings
			runBlocking {
				Glob.settings.architectures.forEach { arch ->
					Glob.message("Getting checksum for $arch...")
					val response = Glob.settings.upstream.checksumUrl.replace("__REPLACE_WITH_ARCHITECTURE__", arch).httpGet()
					(response.statusCode == 200)
						.echt { Glob.message("\t ... was found to be ${String(response.body.bytes()).trim()}") }
						.doch { Glob.exitProcess("Could not get checksum for $arch!", Glob.ExitCodes.COULD_NOT_GET_CHECKSUM_FOR_ARCHITECTURE) }
				}

				File(Glob.settings.git.branches.checksumsDir).isDirectory
					.doch { Glob.exitProcess("Directory for 'git.branches.checksumsDir' supposedly at path '${Glob.settings.git.branches.checksumsDir}' does not exist!", Glob.ExitCodes.DIRECTORY_DOES_NOT_EXIST) }
					.echt { Glob.message("Setting 'git.branches.checksumsDir' OK") }
				File(Glob.settings.git.branches.mainDir).isDirectory
					.doch { Glob.exitProcess("Directory for 'git.branches.mainDir' supposedly at path '${Glob.settings.git.branches.mainDir}' does not exist!", Glob.ExitCodes.DIRECTORY_DOES_NOT_EXIST_gitRepoDir) }
					.echt { Glob.message("Setting 'git.branches.mainDir' OK") }
				(Glob.settings.upstream.repoApiUrl.httpGet().statusCode == 200)
					.doch { Glob.exitProcess("Could not get information for branches!", Glob.ExitCodes.FAILED_GETTING_INFORMATION_ABOUT_BRANCHES) }
					.echt { Glob.message("Setting 'upstream.repoApiUrl' OK") }

			}

			exitProcess(Glob.ExitCodes.ALL_OK.ordinal)
		}

		private fun echoSettingsSkeleton() {
			runBlocking {
				val defaultSettings = Settings()
				val response = defaultSettings.upstream.repoApiUrl.httpGet()

				(response.statusCode != 200).echt {
					Glob.message("Failed getting branches information from '${Settings().upstream.repoApiUrl}!'")
					exitProcess(Glob.ExitCodes.CRON_FAILED_GETTING_BRANCH_NAMES.ordinal)
				}

				val body = String(response.body.bytes())
				val allArchitectures = Json.decodeFromString<List<Branch>>(body)
					.filter { it.name.startsWith(defaultSettings.upstream.repoBranchPrefix) }
					.map { it.name.replace(defaultSettings.upstream.repoBranchPrefix, "") }
					.toSet()

				val settings = Settings(architectures = allArchitectures)

				val json = Json {
					prettyPrint = true
					encodeDefaults = true
				}

				echo(json.encodeToString(settings))
			}

			exitProcess(Glob.ExitCodes.ALL_OK.ordinal)
		}
	}
}