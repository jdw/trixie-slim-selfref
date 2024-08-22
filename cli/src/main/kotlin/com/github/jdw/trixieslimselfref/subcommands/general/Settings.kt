package com.github.jdw.trixieslimselfref.subcommands.general

import com.github.jdw.trixieslimselfref.Glob
import kotlinx.serialization.Serializable

@Serializable
data class Settings(
	val webhook: Webhook = Webhook(),
	val architectures: Set<String> = emptySet(),
	val upstream: UpstreamSettings = UpstreamSettings(),
	val git: GitSettings = GitSettings(),
	val shell: String = "/bin/bash"
)

@Serializable
data class GitSettings(
	val commands: GitCommandsSettings = GitCommandsSettings(),
	val branches: GitBranchesSettings = GitBranchesSettings()
)

@Serializable
data class GitCommandsSettings(
	val commitChecksums: String = """git commit -m "Found new checksum(s) on the $(date +%F)" """,
	val addAll: String = "git add --all",
	val pushOrigin: String = "git push origin",
	val pull: String = "git pull",
	val countChecksumChanges: String = """git diff | grep "+++" | wc -l"""
)

@Serializable
data class GitBranchesSettings(
	val mainDir: String = "${Glob.baseDir}/github.com/jdw/trixie-slim-selfref-branch-main",
	val checksumsDir: String = "${Glob.baseDir}/github.com/jdw/trixie-slim-selfref-branch-checksums"
)

@Serializable
data class UpstreamSettings(
	val checksumUrl: String = "https://raw.githubusercontent.com/debuerreotype/docker-debian-artifacts/dist-__REPLACE_WITH_ARCHITECTURE__/trixie/slim/rootfs.tar.xz.sha256",
	val repoApiUrl: String = "https://api.github.com/repos/debuerreotype/docker-debian-artifacts/branches",
	val repoBranchPrefix: String = "dist-"
)

@Serializable
data class Webhook(
	val url: String = "",
	val username: String = "Captain Kotlin",
)