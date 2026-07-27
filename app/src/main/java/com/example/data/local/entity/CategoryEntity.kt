package com.example.data.local.entity

data class CategoryEntity(
    val id: String = "",
    val name: String = "",
    val color: String = "#A5AEDB"
)

const val FALLBACK_CATEGORY_COLOR = "#A5AEDB"
const val FALLBACK_CATEGORY_LABEL = "Sem categoria"

/** Seeded once per user, na primeira vez que a lista de categorias estiver vazia. */
val DEFAULT_CATEGORIES: List<CategoryEntity> = listOf(
    CategoryEntity(name = "Comida & Salgados", color = "#FF8A80"),
    CategoryEntity(name = "Bebidas", color = "#4FD1C5"),
    CategoryEntity(name = "Decoração", color = "#FFC94D"),
    CategoryEntity(name = "Aluguel do Local", color = "#9B7EE8"),
    CategoryEntity(name = "Som & Animação", color = "#FF9AC6"),
    CategoryEntity(name = "Outros", color = "#A5AEDB"),
)

/** Paleta de cores oferecida ao criar/editar uma categoria. */
val CATEGORY_COLOR_SWATCHES: List<String> = listOf(
    "#FF8A80", "#4FD1C5", "#FFC94D", "#9B7EE8", "#FF9AC6", "#A5AEDB",
    "#60A5FA", "#34D399", "#FBBF24", "#F472B6", "#A78BFA", "#F87171",
)
