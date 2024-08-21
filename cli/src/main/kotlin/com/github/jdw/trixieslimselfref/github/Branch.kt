package com.github.jdw.trixieslimselfref.github

import kotlinx.serialization.Serializable

@Serializable
data class Branch(val name: String, val commit: Commit, val protected: Boolean)