package com.github.jdw.trixieslimselfref.subcommands.general

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
	val webhookUrl: String = "",
	val webhookUsername: String = "Captain Kotlin"
)