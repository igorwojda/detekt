package io.gitlab.arturbosch.detekt.formatting

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.formatting.wrappers.Filename
import io.gitlab.arturbosch.detekt.formatting.wrappers.PackageName
import io.gitlab.arturbosch.detekt.test.assertThat
import org.junit.jupiter.api.Test

class RulesWhichCantBeCorrectedSpec {

    @Test
    fun `verify findings of these rules are not correctable`() {
        val code = """
            package under_score
            class NotTheFilename
        """.trimIndent()

        assertThat(Filename(Config.empty).lint(code))
            .isNotEmpty
            .hasExactlyElementsOfTypes(CodeSmell::class.java)
        assertThat(PackageName(Config.empty).lint(code))
            .isNotEmpty
            .hasExactlyElementsOfTypes(CodeSmell::class.java)
    }
}
