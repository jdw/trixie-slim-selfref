package com.github.jdw.trixieslimselfref.subcommands.git

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class Git: CliktCommand(help="Git tasks subcommand", printHelpOnEmptyArgs = true) {
	private val pullChecksumsBranch by option(help = "Pulls the checksums branch.").flag()
	private val pullMainBranch by option(help = "Pulls the main branch").flag()

	override fun run() {

	}
}