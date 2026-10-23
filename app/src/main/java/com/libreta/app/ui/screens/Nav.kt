package com.libreta.app.ui.screens

/** Acciones de navegación que comparten las pantallas. */
data class Nav(
    val openNote: (String) -> Unit,
    val newNote: (start: String?, notebookId: String?, tagId: String?) -> Unit,
    val openNotes: (nb: String?, tag: String?, filter: String?) -> Unit,
    val openSearch: (String?) -> Unit,
    val go: (String) -> Unit,
    val back: () -> Unit,
    val openCreate: () -> Unit,
    val newFromTemplate: () -> Unit,
)
