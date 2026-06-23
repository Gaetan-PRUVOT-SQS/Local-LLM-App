package com.gaetan.localllmapp

import com.gaetan.localllmapp.data.Skills
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillsTest {

    @Test
    fun registry_isNotEmpty() {
        assertTrue(Skills.all.isNotEmpty())
    }

    @Test
    fun allSkills_haveUniqueIds() {
        val ids = Skills.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun allSkills_haveNonBlankPromptPrefix() {
        Skills.all.forEach { skill ->
            assertTrue("Skill ${skill.id} doit avoir un prompt", skill.promptPrefix.isNotBlank())
            assertTrue("Skill ${skill.id} doit avoir un nom", skill.name.isNotBlank())
        }
    }

    @Test
    fun byId_resolvesKnownSkill() {
        assertNotNull(Skills.byId("traduction"))
        assertEquals("Traduction", Skills.byId("traduction")?.name)
    }

    @Test
    fun byId_returnsNullForUnknown() {
        assertNull(Skills.byId("inconnu"))
        assertNull(Skills.byId(null))
    }
}
