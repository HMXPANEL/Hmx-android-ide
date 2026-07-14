/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.hmxpanel.aiide.templates.impl.composeActivity

import com.hmxpanel.aiide.templates.Language.Kotlin
import com.hmxpanel.aiide.templates.ProjectVersionData
import com.hmxpanel.aiide.templates.base.composeDependencies
import com.hmxpanel.aiide.templates.base.modules.android.defaultAppModule
import com.hmxpanel.aiide.templates.base.util.AndroidModuleResManager.ResourceType.VALUES
import com.hmxpanel.aiide.templates.impl.R
import com.hmxpanel.aiide.templates.impl.base.createRecipe
import com.hmxpanel.aiide.templates.impl.base.writeMainActivity
import com.hmxpanel.aiide.templates.impl.baseProjectImpl
import com.hmxpanel.aiide.templates.projectLanguageParameter

private const val composeKotlinVersion = "1.7.20"

private fun composeLanguageParameter() = projectLanguageParameter {
  default = Kotlin
  filter = { it == Kotlin }
}

// Compose template is available only in Kotlin
fun composeActivityProject() =
  baseProjectImpl(language = composeLanguageParameter(),
    projectVersionData = ProjectVersionData(kotlin = composeKotlinVersion)) {

    templateName = R.string.template_compose
    thumb = R.drawable.template_compose_empty_activity

    defaultAppModule(addAndroidX = false) {

      isComposeModule = true

      recipe = createRecipe {

        require(
          data.language == Kotlin) { "Compose activity requires Kotlin language" }

        composeDependencies()

        res {
          writeXmlResource("themes", VALUES, source = ::composeThemesXml)
        }

        sources {
          writeMainActivity(this, ktSrc = ::composeActivitySrc,
            javaSrc = { "" })
          writeKtSrc("${data.packageName}.ui.theme", "Color",
            source = ::themeColorSrc)
          writeKtSrc("${data.packageName}.ui.theme", "Theme",
            source = ::themeThemeSrc)
          writeKtSrc("${data.packageName}.ui.theme", "Type",
            source = ::themeTypeSrc)
        }
      }
    }
  }