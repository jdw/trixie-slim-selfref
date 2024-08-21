package com.github.jdw.trixieslimselfref.subcommands

import kotlinx.serialization.Serializable

const val BASE_DIR = "~/.trixie-slim-selfref/cron"

@Serializable
data class Settings(
	val architectures: Set<String> = emptySet(),
	val gitChecksumsDir: String = "$BASE_DIR/github.com-jdw-trixie-slim-selfref-branch-checksums/checksums",
	val gitRepoDir: String = "$BASE_DIR/github.com-jdw-trixie-slim-selfref-branch-main",
	val upstreamChecksumUrl: String = "https://raw.githubusercontent.com/debuerreotype/docker-debian-artifacts/dist-__REPLACE_WITH_ARCHITECTURE__/trixie/slim/rootfs.tar.xz.sha256",
	val upstreamRepoApiUrl: String = "https://api.github.com/repos/debuerreotype/docker-debian-artifacts/branches",
	val upstreamRepoBranchPrefix: String = "dist-"
)