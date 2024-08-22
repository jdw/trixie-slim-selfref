package com.github.jdw.trixieslimselfref.subcommands.git

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.jdw.trixieslimselfref.Glob
import com.jaredrummler.ktsh.Shell
import doch
import echt
import kotlin.system.exitProcess

class Git: CliktCommand(help="Git tasks subcommand", printHelpOnEmptyArgs = true) {
	private val baseDirWithTilde = Glob.baseDir.replace(System.getenv("HOME"), "~")
	private val pullChecksumsBranch by option(help = "Pulls the checksums branch in the '$baseDirWithTilde' directory").flag()
	private val pullMainBranch by option(help = "Pulls the main branch in the '$baseDirWithTilde' directory").flag()

	override fun run() {
		pullChecksumsBranch.echt { pullBranch(Glob.settings.git.branches.checksumsDir) }
		pullMainBranch.echt { pullBranch(Glob.settings.git.branches.mainDir) }
	}

	companion object {
		private fun pullBranch(dir: String) {
			val bash = Shell(Glob.settings.shell)
			bash.run("cd $dir").also { result ->
				(result.exitCode == 0)
					.echt { Glob.message("Changed PWD to '$dir'...") }
					.doch { Glob.exitProcess("Failed to change PWD to '$dir}'", Glob.ExitCodes.FAILED_ENTER_DIR) }
			}


			exitProcess(Glob.ExitCodes.ALL_OK.ordinal)
		}
	}
}