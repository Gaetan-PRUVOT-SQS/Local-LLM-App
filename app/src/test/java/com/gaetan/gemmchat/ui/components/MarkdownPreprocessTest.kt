package com.gaetan.gemmchat.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests JVM purs du pré-traitement LaTeX/maths (aucun device requis).
 * Couvre les cas réels émis par le modèle (cf. audit QA) + robustesse streaming.
 */
class MarkdownPreprocessTest {

    @Test fun stripsInlineTextWrapper() {
        assertEquals("La molécule G3P est clé.", cleanupMath("La molécule \$\\text{G3P}\$ est clé."))
    }

    @Test fun subscriptDigitsToUnicode() {
        assertEquals("Formule O₂ et CO₂.", cleanupMath("Formule \$\\text{O}_2\$ et \$\\text{CO}_2\$."))
    }

    @Test fun waterFormula() {
        assertEquals("H₂O", cleanupMath("\$\\text{H}_2\\text{O}\$"))
    }

    @Test fun superscriptBraces() {
        assertEquals("x² + y²", cleanupMath("\$x^{2}\$ + \$y^2\$"))
    }

    @Test fun ionCharge() {
        assertEquals("Ca²⁺", cleanupMath("\$\\text{Ca}^{2+}\$"))
    }

    @Test fun parenDelimiterAndTimes() {
        assertEquals("a × b", cleanupMath("\\(a \\times b\\)"))
    }

    @Test fun bracketDisplayDelimiter() {
        assertEquals("E = mc²", cleanupMath("\\[E = mc^2\\]"))
    }

    @Test fun greekAndArrow() {
        assertEquals("α → β", cleanupMath("\$\\alpha \\to \\beta\$"))
    }

    @Test fun removesFootnoteRefs() {
        assertEquals("Un fait important.", cleanupMath("Un fait important[^1]."))
    }

    @Test fun keepsCurrencyDollars() {
        // Pas de marqueur LaTeX entre les $ → laissé intact (pas de maths).
        assertEquals("entre 5\$ et 10\$", cleanupMath("entre 5\$ et 10\$"))
    }

    @Test fun streamingUnclosedDollarIsLiteral() {
        // Délimiteur non fermé en cours de streaming → jamais transformé.
        assertEquals("calcul de \$\\text{O", cleanupMath("calcul de \$\\text{O"))
    }

    @Test fun plainTextUnchanged() {
        assertEquals("Bonjour, comment ça va ?", cleanupMath("Bonjour, comment ça va ?"))
    }

    @Test fun emptyStays() {
        assertEquals("", cleanupMath(""))
    }

    @Test fun genericCommandKeepsArgument() {
        assertEquals("v", cleanupMath("\$\\vec{v}\$"))
    }

    @Test fun sqrtSymbolPreserved() {
        assertEquals("√2", cleanupMath("\$\\sqrt{2}\$"))
    }
}
