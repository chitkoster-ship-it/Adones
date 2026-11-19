package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable
    data object Terminal : AppDestination

    @Serializable
    data object Strategies : AppDestination

    @Serializable
    data object Journal : AppDestination

    @Serializable
    data object Monitor : AppDestination
}
