package com.github.jdw.trixieslimselfref

import doch
import echt

object Glob { // The global object
	var verbose = false

	enum class ExitCodes { // Never change the order of these, never delete an entry, only add
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
		CRON_FAILED_PULLING_CHECKSUMS // exit code 19
	}

	fun message(msg: String, error: Boolean = false) {
		error
			.echt { println("ERROR :: $msg") }
			.doch { Glob.verbose.echt { println(msg) } }
	}

	fun exitProcess(errorMsg: String, exitCode: ExitCodes): Nothing {
		Glob.message(errorMsg, true)
		kotlin.system.exitProcess(exitCode.ordinal)
	}
}