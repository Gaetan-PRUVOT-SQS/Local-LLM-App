package com.gaetan.gemmchat.ui.components

/**
 * Pré-traitement texte PUR (aucune dépendance Compose/Android → testable en JVM).
 *
 * Le modèle émet régulièrement du LaTeX/maths que le rendu markdown maison ne
 * gère pas (`$\text{O}_2$`, `\(a \times b\)`, indices/exposants, notes `[^1]`),
 * affiché jusqu'ici en brut. [cleanupMath] le convertit en texte lisible
 * (symboles Unicode, indices/exposants Unicode) avant le parsing inline.
 *
 * Robustesse streaming : un délimiteur non fermé (`$` seul) n'est jamais
 * transformé → laissé littéral, jamais de crash. Les `$...$` qui ne ressemblent
 * pas à des maths (ex. montants « 5$ et 10$ ») sont laissés intacts.
 */
internal fun cleanupMath(input: String): String {
    if (input.isEmpty()) return input
    var s = input

    // Maths affichées $$...$$ puis en ligne $...$ — uniquement si le contenu
    // contient un marqueur LaTeX (\, _, ^, {) pour ne pas casser les montants.
    s = MATH_DISPLAY.replace(s) { m -> if (looksLikeMath(m.groupValues[1])) renderMath(m.groupValues[1]) else m.value }
    s = MATH_INLINE.replace(s) { m -> if (looksLikeMath(m.groupValues[1])) renderMath(m.groupValues[1]) else m.value }
    // Délimiteurs LaTeX non ambigus \( ... \) et \[ ... \]
    s = MATH_PAREN.replace(s) { renderMath(it.groupValues[1]) }
    s = MATH_BRACK.replace(s) { renderMath(it.groupValues[1]) }
    // Références de notes markdown [^1]
    s = FOOTNOTE.replace(s, "")
    return s
}

private fun looksLikeMath(inner: String): Boolean =
    inner.any { it == '\\' || it == '_' || it == '^' || it == '{' }

/** Convertit une expression LaTeX en texte Unicode au mieux. */
private fun renderMath(expr: String): String {
    var s = expr
    // \text{...}, \mathrm{...}, etc. → contenu
    s = TEXT_WRAP.replace(s) { it.groupValues[1] }
    // Symboles courants (avant le strip générique pour préserver \sqrt, etc.)
    for ((k, v) in SYMBOLS) s = s.replace(k, v)
    // Toute autre commande à argument \cmd{arg} → arg (ex. \vec{v} → v)
    s = GENERIC_WRAP.replace(s) { it.groupValues[1] }
    // Indices et exposants (accolades puis caractère seul)
    s = SUB_BRACE.replace(s) { toScript(it.groupValues[1], SUBSCRIPT) }
    s = SUB_CHAR.replace(s) { toScript(it.groupValues[1], SUBSCRIPT) }
    s = SUP_BRACE.replace(s) { toScript(it.groupValues[1], SUPERSCRIPT) }
    s = SUP_CHAR.replace(s) { toScript(it.groupValues[1], SUPERSCRIPT) }
    // Espacements LaTeX
    s = s.replace("\\,", " ").replace("\\;", " ").replace("\\:", " ")
        .replace("\\!", "").replace("\\\\", " ").replace("\\ ", " ")
    // Commandes \xxx restantes → nom sans antislash
    s = LEFTOVER_CMD.replace(s) { it.groupValues[1] }
    // Accolades résiduelles
    s = s.replace("{", "").replace("}", "")
    // Espaces multiples
    s = MULTISPACE.replace(s, " ")
    return s.trim()
}

private fun toScript(text: String, map: Map<Char, Char>): String =
    buildString { for (c in text) append(map[c] ?: c) }

private val MATH_DISPLAY = Regex("""\$\$(.+?)\$\$""", RegexOption.DOT_MATCHES_ALL)
private val MATH_INLINE = Regex("""\$([^\$\n]+?)\$""")
private val MATH_PAREN = Regex("""\\\((.+?)\\\)""", RegexOption.DOT_MATCHES_ALL)
private val MATH_BRACK = Regex("""\\\[(.+?)\\\]""", RegexOption.DOT_MATCHES_ALL)
private val FOOTNOTE = Regex("""\[\^[^\]]*\]""")
private val TEXT_WRAP = Regex("""\\(?:text|mathrm|mathbf|mathit|mathsf|operatorname)\s*\{([^}]*)\}""")
private val GENERIC_WRAP = Regex("""\\[A-Za-z]+\s*\{([^}]*)\}""")
private val SUB_BRACE = Regex("""_\{([^}]*)\}""")
private val SUB_CHAR = Regex("""_([0-9A-Za-z+\-=()])""")
private val SUP_BRACE = Regex("""\^\{([^}]*)\}""")
private val SUP_CHAR = Regex("""\^([0-9A-Za-z+\-=()])""")
private val LEFTOVER_CMD = Regex("""\\([A-Za-z]+)""")
private val MULTISPACE = Regex(" {2,}")

private val SYMBOLS = linkedMapOf(
    "\\times" to "×", "\\cdot" to "·", "\\div" to "÷", "\\pm" to "±", "\\mp" to "∓",
    "\\leftrightarrow" to "↔", "\\rightarrow" to "→", "\\leftarrow" to "←",
    "\\Rightarrow" to "⇒", "\\Leftarrow" to "⇐", "\\to" to "→",
    "\\approx" to "≈", "\\neq" to "≠", "\\equiv" to "≡",
    "\\leq" to "≤", "\\geq" to "≥", "\\le" to "≤", "\\ge" to "≥",
    "\\infty" to "∞", "\\degree" to "°", "\\circ" to "°",
    "\\sum" to "∑", "\\prod" to "∏", "\\sqrt" to "√", "\\int" to "∫",
    "\\partial" to "∂", "\\nabla" to "∇", "\\propto" to "∝",
    "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
    "\\epsilon" to "ε", "\\theta" to "θ", "\\lambda" to "λ", "\\mu" to "µ",
    "\\pi" to "π", "\\rho" to "ρ", "\\sigma" to "σ", "\\tau" to "τ",
    "\\phi" to "φ", "\\omega" to "ω",
    "\\Delta" to "Δ", "\\Sigma" to "Σ", "\\Omega" to "Ω", "\\Pi" to "Π",
)

private val SUPERSCRIPT = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵',
    '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹', '+' to '⁺', '-' to '⁻',
    '=' to '⁼', '(' to '⁽', ')' to '⁾', 'n' to 'ⁿ', 'i' to 'ⁱ',
)

private val SUBSCRIPT = mapOf(
    '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄', '5' to '₅',
    '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉', '+' to '₊', '-' to '₋',
    '=' to '₌', '(' to '₍', ')' to '₎', 'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ',
    'x' to 'ₓ', 'h' to 'ₕ', 'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ',
    'p' to 'ₚ', 's' to 'ₛ', 't' to 'ₜ',
)
