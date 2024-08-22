package com.github.jdw.trixieslimselfref.subcommands.general

import kotlinx.serialization.Serializable

@Serializable
data class DiscordWebhookMessage(val content: String, val username: String)
