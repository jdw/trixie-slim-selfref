package com.github.jdw.trixieslimselfref.subcommands.upstream

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.jdw.trixieslimselfref.Glob
import com.jaredrummler.ktsh.Shell
import doch
import echt
import fuel.httpGet
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

class Upstream(): CliktCommand(help = "Upstream management subcommand", printHelpOnEmptyArgs = true) {
	private val checkForNewChecksums by option(help="Check upstream for new checksums. If found applies and pushes them.").flag()

	override fun run() {
		checkForNewChecksums.echt { checkForNewChecksums() }
	}

	companion object {
		private fun checkForNewChecksums() {
			val (settings, _) = Glob.loadSettings()

			runBlocking {
				settings.architectures.forEach { arch ->
					Glob.message("Getting checksum for $arch...")
					val response = settings.upstream.checksumUrl.replace("__REPLACE_WITH_ARCHITECTURE__", arch).httpGet()
					(response.statusCode == 200)
						.echt {
							val bash = Shell("/bin/bash")
							val file = File("${settings.git.branches.checksumsDir}/checksums/$arch.sha256")
							val fetchedChecksum = String(response.body.bytes()).trim()
							val storedChecksum = if (file.exists()) file.readText().trim() else "File '${file.absolutePath}' not found!"

							(storedChecksum == fetchedChecksum)
								.echt { Glob.message("\t ... was found to be same as stored.") }
								.doch {
									bash.run("""echo "$fetchedChecksum" > ${settings.git.branches.checksumsDir}/checksums/$arch.sha256""")
										.also { result ->
											(result.exitCode == 0)
												.echt { Glob.message("\t ... was successfully updated to $fetchedChecksum") }
												.doch {
													Glob.exitProcess(
														"Failed updating found checksum for $arch",
														Glob.ExitCodes.FAILED_SAVING_CHECKSUM
													)
												}
										}
								}
						}
						.doch { Glob.exitProcess("Could not get checksum for $arch!", Glob.ExitCodes.COULD_NOT_GET_CHECKSUM_FOR_ARCHITECTURE) }
				}
			}

			val bash = Shell("/bin/bash")

			data class Commandos(val cmd: String, val failureMsg: String, val successFun: (Shell.Command.Result) -> Unit, val exitCodeOnFailure: Glob.ExitCodes, val shouldRunFun: () -> Boolean)
			var amountOfDiffingFiles = 0
			listOf(
				Commandos(
					"cd ${settings.git.branches.checksumsDir}",
					"Failed to enter directory '${settings.git.branches.checksumsDir}'!",
					{ _ -> Glob.message("Entered directory '${settings.git.branches.checksumsDir}'!") },
					Glob.ExitCodes.FAILED_ENTER_DIR,
					{ true }
				),
				Commandos(
					settings.git.commands.countChecksumChanges,
					"Failed getting diff of repository!",
					{ result ->
						amountOfDiffingFiles = result.output().trim().toInt()
						val message = "Found $amountOfDiffingFiles new checksum${ if (amountOfDiffingFiles == 1) "" else "s" }..."
						if (Glob.verbose) Glob.webhookMessage(message)
						Glob.message(message)
					},
					Glob.ExitCodes.CRON_FAILED_GETTING_DIFF,
					{ true }
				),
				Commandos(
					settings.git.commands.addAll,
					"Failed to run '${settings.git.commands.addAll}' in '${settings.git.branches.checksumsDir}'!",
					{ _ -> Glob.message("Successfully added all new checksums to git") },
					Glob.ExitCodes.GIT_COMMAND_FAILED,
					{ amountOfDiffingFiles != 0 }
				),
				Commandos(
					"""git commit -m "Found new checksum${ if (amountOfDiffingFiles == 1) "" else "s" } on the $(date +%F)" && git push origin""",
					"Failed to commit and push checksums!",
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
	}
}