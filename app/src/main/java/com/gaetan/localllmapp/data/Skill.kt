package com.gaetan.localllmapp.data

/**
 * Skill = preset de prompt. Sélectionnée, elle préfixe le message de l'utilisateur avec
 * [promptPrefix] avant l'envoi au modèle (le message affiché reste, lui, inchangé).
 * 100% local, aucune dépendance externe.
 */
data class Skill(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val promptPrefix: String,
)

object Skills {
    val all: List<Skill> = listOf(
        Skill(
            id = "resume",
            name = "Résumé",
            emoji = "📝",
            description = "Synthétise en points clés",
            promptPrefix = "Résume le texte suivant en quelques points clés, concis et clairs :\n\n",
        ),
        Skill(
            id = "traduction",
            name = "Traduction",
            emoji = "🌐",
            description = "Traduit en anglais",
            promptPrefix = "Traduis fidèlement le texte suivant en anglais. Renvoie uniquement la traduction :\n\n",
        ),
        Skill(
            id = "correction",
            name = "Correction",
            emoji = "✅",
            description = "Orthographe & style",
            promptPrefix = "Corrige l'orthographe, la grammaire et améliore le style du texte suivant. Renvoie uniquement la version corrigée :\n\n",
        ),
        Skill(
            id = "code",
            name = "Code",
            emoji = "💻",
            description = "Génère ou explique du code",
            promptPrefix = "Tu es un assistant de programmation expert. Réponds avec du code clair, correct et commenté.\n\nDemande : ",
        ),
        Skill(
            id = "brainstorm",
            name = "Brainstorm",
            emoji = "💡",
            description = "Génère des idées",
            promptPrefix = "Propose une liste d'idées créatives, variées et concrètes pour :\n\n",
        ),
        Skill(
            id = "eli5",
            name = "Explication simple",
            emoji = "🧒",
            description = "Explique simplement",
            promptPrefix = "Explique de façon très simple et imagée, comme à un enfant de 10 ans :\n\n",
        ),
    )

    fun byId(id: String?): Skill? = id?.let { wanted -> all.firstOrNull { it.id == wanted } }
}
