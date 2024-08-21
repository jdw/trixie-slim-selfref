package com.github.jdw.trixieslimselfref.subcommands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.jdw.trixieslimselfref.Glob
import com.github.jdw.trixieslimselfref.github.Branch
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
	private val checkChecksums by option(help="Check upstream for new checksums. If found applies and pushes them.").flag()
	val settingsDefaultDir = "${System.getenv("HOME")}/.trixie-slim-selfref/cron"
	private val checkSettings by option(help="Checks correctness of settings").flag()
	private val settingsDir by argument(help = "The directory to check for settings").default(settingsDefaultDir)
	private val echoSettingsSkeleton by option(help = "Pretty prints a default settings file for this subcommand and exits.").flag()

	override fun run() {
		(!checkChecksums && !checkSettings && !echoSettingsSkeleton).echt {
			Glob.message("Nothing to do!", true)
			System.exit(Glob.ExitCodes.CRON_NOTHING_TO_DO.ordinal)
		}

		echoSettingsSkeleton.echt { echoSettingsSkeleton() }
		checkSettings.echt { checkSettings(settingsDir) }
	}

	companion object {
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

			exitProcess(0)
		}

		private fun checkSettings(dir: String) {
			File(dir).isDirectory.doch {
				println("Settings directory ($dir) not found!")
				exitProcess(Glob.ExitCodes.CRON_NO_SETTINGS_DIR.ordinal)
			}

			val settingsFile = File("$dir/settings.json")
			settingsFile.isFile.doch {
				println("File '${settingsFile.absoluteFile}' not found!")
				exitProcess(Glob.ExitCodes.CRON_NO_SETTINGS_FILE.ordinal)
			}

			val settings = try {
				Json.decodeFromString<Settings>(settingsFile.readText())
			}
			catch (_: Exception) {
				Glob.message("Failed to parse file '${settingsFile.absoluteFile}'!", true)
				exitProcess(Glob.ExitCodes.CRON_SETTINGS_FILE_PARSE_FAIL.ordinal)
			}

			// Checking existence of settings
			settings.architectures.isEmpty().echt {
				Glob.message("No architectures in settings file ('${settingsFile.absoluteFile}')!", true)
				exitProcess(Glob.ExitCodes.CRON_SETTINGS_NO_ARCHITECTURES_IN_SETTINGS_FILE.ordinal)
			}

			settings.gitChecksumsDir.isEmpty().echt {
				Glob.message("No data in 'gitChecksumsDir' in settings file ('${settingsFile.absoluteFile}')!", true)
				exitProcess(Glob.ExitCodes.CRON_SETTINGS_BAD_DATA_gitChecksumsDir.ordinal)
			}

			settings.gitRepoDir.isEmpty().echt {
				Glob.message("No data in 'gitRepoDir' in settings file ('${settingsFile.absoluteFile}')!", true)
				exitProcess(Glob.ExitCodes.CRON_SETTINGS_BAD_DATA_gitRepoDir.ordinal)
			}

			settings.upstreamChecksumUrl.isEmpty().echt {
				Glob.message("No data in 'upstreamChecksumUrl' in settings file ('${settingsFile.absoluteFile}')!", true)
				exitProcess(Glob.ExitCodes.CRON_SETTINGS_BAD_DATA_upstreamChecksumUrl.ordinal)
			}

			settings.upstreamRepoApiUrl.isEmpty().echt {
				Glob.message("No data in 'upstreamRepoApiUrl' in settings file ('${settingsFile.absoluteFile}')!", true)
				exitProcess(Glob.ExitCodes.CRON_SETTINGS_BAD_DATA_upstreamRepoApiUrl.ordinal)
			}

			settings.upstreamRepoBranchPrefix.isEmpty().echt {
				Glob.message("No data in 'upstreamRepoBranchPrefix' in settings file ('${settingsFile.absoluteFile}')!", true)
				exitProcess(Glob.ExitCodes.CRON_SETTINGS_BAD_DATA_upstreamRepoBranchPrefix.ordinal)
			}

			// Checking validity of settings
			runBlocking {
				settings.architectures.forEach { arch ->
					launch {
						Glob.message("Getting checksum for '$arch'...")
						val response = settings.upstreamChecksumUrl.replace("__REPLACE_WITH_ARCHITECTURE__", arch).httpGet()
						(response.statusCode == 200)
							.echt {
								Glob.message("Checksum for '$arch' = '${String(response.body.bytes()).trim()}'.")
							}
							.doch {
								Glob.message("Could not get checksum for architecture '$arch'!")
								exitProcess(Glob.ExitCodes.Could_not_get_checksum_for_architecture.ordinal)
							}
					}
				}

				launch {
					val gitChecksumsDir = settings.gitChecksumsDir.replace("~", System.getenv("HOME"))
					File(gitChecksumsDir).isDirectory.doch {
						Glob.message("Directory for 'gitChecksumsDir' supposedly at path '${gitChecksumsDir}' does not exist!", true)
						exitProcess(Glob.ExitCodes.DIRECTORY_DOES_NOT_EXIST_gitChecksumsDir.ordinal)
					}
				}

				launch {
					val gitRepoDir = settings.gitRepoDir.replace("~", System.getenv("HOME"))
					File(gitRepoDir).isDirectory.doch {
						Glob.message("Directory for 'gitChecksumsDir' supposedly at path '${gitRepoDir}' does not exist!", true)
						exitProcess(Glob.ExitCodes.DIRECTORY_DOES_NOT_EXIST_gitRepoDir.ordinal)
					}
				}

				launch {
					val response = settings.upstreamRepoApiUrl.httpGet()
					(response.statusCode == 200)
						.doch {
							Glob.message("Could not get information for branches!")
							exitProcess(Glob.ExitCodes.Could_not_get_information_about_branches.ordinal)
						}
				}
			}

			exitProcess(Glob.ExitCodes.ALL_OK.ordinal)
		}
	}
}