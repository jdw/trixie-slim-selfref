package com.github.jdw.trixieslimselfref.github

import kotlinx.serialization.Serializable

@Serializable
data class Commit(val sha: String, val url: String)