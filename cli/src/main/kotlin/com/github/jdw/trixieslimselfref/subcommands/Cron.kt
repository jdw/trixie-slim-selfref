package com.github.jdw.trixieslimselfref.subcommands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.jdw.trixieslimselfref.Glob
import com.github.jdw.trixieslimselfref.github.Branch
import com.jaredrummler.ktsh.Shell
import doch
import echt
import fuel.httpGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

class Cron(): CliktCommand(help = "Crontab management subcommand") {
	private val checkForNewChecksums by option(help="Check upstream for new checksums. If found applies and pushes them.").flag()
	val settingsDefaultDir = "${System.getenv("HOME")}/.trixie-slim-selfref/cron"
	private val checkSettings by option(help="Checks correctness of settings").flag()
	private val settingsDir by argument(help = "The directory to check for settings").default(settingsDefaultDir)
	private val echoSettingsSkeleton by option(help = "Pretty prints a default settings file for this subcommand and exits.").flag()

	override fun run() {
		(!checkForNewChecksums && !checkSettings && !echoSettingsSkeleton).echt {
			Glob.message("Nothing to do!", true)
			exitProcess(Glob.ExitCodes.CRON_NOTHING_TO_DO.ordinal)
		}

		echoSettingsSkeleton.echt { echoSettingsSkeleton() }
		checkSettings.echt { checkSettings(settingsDir) }
		checkForNewChecksums.echt { checkForNewChecksums(settingsDir) }
	}

	companion object {
		private fun loadSettings(filename: String): Settings {
			val settingsFile = File(filename)
			settingsFile.isFile.doch {
				println("File '${settingsFile.absoluteFile}' not found!")
				exitProcess(Glob.ExitCodes.CRON_NO_SETTINGS_FILE.ordinal)
			}

			return try {
				Json.decodeFromString<Settings>(settingsFile.readText())
			}
			catch (_: Exception) {
				Glob.message("Failed to parse file '${settingsFile.absoluteFile}'!", true)
				exitProcess(Glob.ExitCodes.CRON_SETTINGS_FILE_PARSE_FAIL.ordinal)
			}
		}


		private fun checkForNewChecksums(dir: String) {
			val settings = loadSettings("$dir/settings.json")

			runBlocking {
				settings.architectures.forEach { arch ->
					launch {
						Glob.message("Getting checksum for '$arch'...")
						val response = settings.upstreamChecksumUrl.replace("__REPLACE_WITH_ARCHITECTURE__", arch).httpGet()
						(response.statusCode == 200)
							.echt {
								val checksum = String(response.body.bytes()).trim()

								Shell("/bin/bash").run("""echo "$checksum" > ${settings.gitChecksumsDir}/$arch.sha256""").also { result ->
									(result.exitCode == 0)
										.echt { Glob.message("Successfully updated checksum for $arch to $checksum") }
										.doch {
											Glob.message("Failed updating found checksum for $arch", true)
											exitProcess(Glob.ExitCodes.CRON_FAILED_SAVING_CHECKSUM.ordinal)
										}
								}
							}
							.doch { Glob.exitProcess("Could not get checksum for architecture '$arch'!", Glob.ExitCodes.Could_not_get_checksum_for_architecture) }
					}
				}
			}

			val checksumsBranchBasedir = settings.gitChecksumsDir.replace("/checksums", "")
			val bash = Shell("/bin/bash")

			data class Commandos(val cmd: String, val failureMsg: String, val successFun: (Shell.Command.Result) -> Unit, val exitCodeOnFailure: Glob.ExitCodes, val shouldRunFun: () -> Boolean)
			var amountOfDiffingFiles = 0
			listOf(
				Commandos(
					"cd $checksumsBranchBasedir",
					"Failed to enter directory '$checksumsBranchBasedir'!",
					{ _ -> Glob.message("Entered directory '$checksumsBranchBasedir'!") },
					Glob.ExitCodes.CRON_FAILED_ENTER_CHECKSUMS_BRANCH_DIR,
					{ true }
				),
				Commandos(
					"""git diff | grep "+++" | wc -l""",
					"Failed getting diff of repository!",
					{ result ->
						amountOfDiffingFiles = result.output().trim().toInt()
						Glob.message("Found $amountOfDiffingFiles new checksum${ if (amountOfDiffingFiles == 1) "" else "s" }...") },
					Glob.ExitCodes.CRON_FAILED_GETTING_DIFF,
					{ true }
				),
				Commandos(
					"""git add --all && git commit -m "Found new checksum${ if (amountOfDiffingFiles == 1) "" else "s" } on the $(date +%F)" && git push origin""",
					"Failed to commit and push!",
					{ _ -> Glob.message("Successfully commited and pushed new checksum${ if (amountOfDiffingFiles == 1) "" else "s" }!") },
					Glob.ExitCodes.CRON_FAILED_COMMIT_AND_PUSH_CHECKSUMS,
					{ amountOfDiffingFiles != 0 }
				)
			).forEach { action ->
				(action.shouldRunFun())
					.echt {
						bash.run(action.cmd).also { result ->
							(result.exitCode == 0)
								.echt { action.successFun(result) }
								.doch { Glob.exitProcess(action.failureMsg, action.exitCodeOnFailure) }
						}
					}
			}

			exitProcess(Glob.ExitCodes.ALL_OK.ordinal)
		}

		private fun echoSettingsSkeleton() {
			runBlocking {
				val defaultSettings = Settings()
				val response = defaultSettings.upstreamRepoApiUrl.httpGet()

				(response.statusCode != 200).echt {
					Glob.message("Failed getting branches information from '${Settings().upstreamRepoApiUrl}!'")
					exitProcess(Glob.ExitCodes.CRON_FAILED_GETTING_BRANCH_NAMES.ordinal)
				}

				val body = String(response.body.bytes())
				val allArchitectures = Json.decodeFromString<List<Branch>>(body)
					.filter { it.name.startsWith(defaultSettings.upstreamRepoBranchPrefix) }
					.map { it.name.replace(defaultSettings.upstreamRepoBranchPrefix, "") }
					.toSet()

				val settings = Settings(architectures = allArchitectures)

				val json = Json {
					prettyPrint = true
					encodeDefaults = true
				}

				println(json.encodeToString(settings, ))
			}

			exitProcess(Glob.ExitCodes.ALL_OK.ordinal)
		}

		private fun checkSettings(dir: String) {
			File(dir).isDirectory.doch {
				println("Settings directory ($dir) not found!")
				exitProcess(Glob.ExitCodes.CRON_NO_SETTINGS_DIR.ordinal)
			}

			val settingsFile = File("$dir/settings.json")
			settingsFile.isFile.doch { Glob.exitProcess("File '${settingsFile.absoluteFile}' not found!", Glob.ExitCodes.CRON_NO_SETTINGS_FILE) }

			val settings = loadSettings("$dir/settings.json")

			// Checking existence of settings
			settings.architectures.isEmpty().echt { Glob.exitProcess("No architectures in settings file ('${settingsFile.absoluteFile}')!", Glob.ExitCodes.CRON_SETTINGS_NO_ARCHITECTURES_IN_SETTINGS_FILE) }
			settings.gitChecksumsDir.isEmpty().echt { Glob.exitProcess("No data in 'gitChecksumsDir' in settings file ('${settingsFile.absoluteFile}')!", Glob.ExitCodes.CRON_SETTINGS_BAD_DATA_gitChecksumsDir) }
			settings.gitRepoDir.isEmpty().echt { Glob.exitProcess("No data in 'gitRepoDir' in settings file ('${settingsFile.absoluteFile}')!", Glob.ExitCodes.CRON_SETTINGS_BAD_DATA_gitRepoDir) }
			settings.upstreamChecksumUrl.isEmpty().echt { Glob.exitProcess("No data in 'upstreamChecksumUrl' in settings file ('${settingsFile.absoluteFile}')!", Glob.ExitCodes.CRON_SETTINGS_BAD_DATA_upstreamChecksumUrl) }
			settings.upstreamRepoApiUrl.isEmpty().echt { Glob.exitProcess("No data in 'upstreamRepoApiUrl' in settings file ('${settingsFile.absoluteFile}')!", Glob.ExitCodes.CRON_SETTINGS_BAD_DATA_upstreamRepoApiUrl) }
			settings.upstreamRepoBranchPrefix.isEmpty().echt { Glob.exitProcess("No data in 'upstreamRepoBranchPrefix' in settings file ('${settingsFile.absoluteFile}')!", Glob.ExitCodes.CRON_SETTINGS_BAD_DATA_upstreamRepoBranchPrefix) }

			// Checking validity of settings
			runBlocking {
				settings.architectures.forEach { arch ->
					launch {
						Glob.message("Getting checksum for '$arch'...")
						val response = settings.upstreamChecksumUrl.replace("__REPLACE_WITH_ARCHITECTURE__", arch).httpGet()
						(response.statusCode == 200)
							.echt { Glob.message("Checksum for '$arch' = '${String(response.body.bytes()).trim()}'.") }
							.doch { Glob.exitProcess("Could not get checksum for architecture '$arch'!", Glob.ExitCodes.Could_not_get_checksum_for_architecture) }
					}
				}

				launch {
					val gitChecksumsDir = settings.gitChecksumsDir.replace("~", System.getenv("HOME"))
					File(gitChecksumsDir).isDirectory.doch { Glob.exitProcess("Directory for 'gitChecksumsDir' supposedly at path '${gitChecksumsDir}' does not exist!", Glob.ExitCodes.DIRECTORY_DOES_NOT_EXIST_gitChecksumsDir) }
				}

				launch {
					val gitRepoDir = settings.gitRepoDir.replace("~", System.getenv("HOME"))
					File(gitRepoDir).isDirectory.doch { Glob.exitProcess("Directory for 'gitChecksumsDir' supposedly at path '${gitRepoDir}' does not exist!", Glob.ExitCodes.DIRECTORY_DOES_NOT_EXIST_gitRepoDir) }
				}

				launch {
					val response = settings.upstreamRepoApiUrl.httpGet()
					(response.statusCode == 200)
						.doch { Glob.exitProcess("Could not get information for branches!", Glob.ExitCodes.Could_not_get_information_about_branches) }
				}
			}

			exitProcess(Glob.ExitCodes.ALL_OK.ordinal)
		}
	}
}