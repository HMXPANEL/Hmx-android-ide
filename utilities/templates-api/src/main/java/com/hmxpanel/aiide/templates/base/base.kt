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

package com.hmxpanel.aiide.templates.base

import com.hmxpanel.aiide.templates.BooleanParameter
import com.hmxpanel.aiide.templates.CheckBoxWidget
import com.hmxpanel.aiide.templates.EnumParameter
import com.hmxpanel.aiide.templates.FileTemplate
import com.hmxpanel.aiide.templates.FileTemplateRecipeResult
import com.hmxpanel.aiide.templates.Language
import com.hmxpanel.aiide.templates.ModuleTemplate
import com.hmxpanel.aiide.templates.ModuleTemplateData
import com.hmxpanel.aiide.templates.ModuleType
import com.hmxpanel.aiide.templates.ModuleType.AndroidApp
import com.hmxpanel.aiide.templates.ModuleType.AndroidLibrary
import com.hmxpanel.aiide.templates.ParameterConstraint.DIRECTORY
import com.hmxpanel.aiide.templates.ParameterConstraint.EXISTS
import com.hmxpanel.aiide.templates.ParameterConstraint.MODULE_NAME
import com.hmxpanel.aiide.templates.ParameterConstraint.NONEMPTY
import com.hmxpanel.aiide.templates.ProjectTemplate
import com.hmxpanel.aiide.templates.ProjectTemplateData
import com.hmxpanel.aiide.templates.ProjectVersionData
import com.hmxpanel.aiide.templates.R
import com.hmxpanel.aiide.templates.Sdk
import com.hmxpanel.aiide.templates.SpinnerWidget
import com.hmxpanel.aiide.templates.StringParameter
import com.hmxpanel.aiide.templates.TextFieldWidget
import com.hmxpanel.aiide.templates.base.util.getNewProjectName
import com.hmxpanel.aiide.templates.base.util.moduleNameToDir
import com.hmxpanel.aiide.templates.enumParameter
import com.hmxpanel.aiide.templates.minSdkParameter
import com.hmxpanel.aiide.templates.packageNameParameter
import com.hmxpanel.aiide.templates.projectLanguageParameter
import com.hmxpanel.aiide.templates.projectNameParameter
import com.hmxpanel.aiide.templates.stringParameter
import com.hmxpanel.aiide.templates.useKtsParameter
import com.hmxpanel.aiide.utils.AndroidUtils
import com.hmxpanel.aiide.utils.Environment
import java.io.File

typealias AndroidModuleTemplateConfigurator = AndroidModuleTemplateBuilder.() -> Unit

/**
 * Setup base files for project templates.
 *
 * @param block Function to configure the template.
 */
inline fun baseProject(projectName: StringParameter = projectNameParameter(),
  packageName: StringParameter = packageNameParameter(),
  useKts: BooleanParameter = useKtsParameter(),
  minSdk: EnumParameter<Sdk> = minSdkParameter(),
  language: EnumParameter<Language> = projectLanguageParameter(),
  projectVersionData: ProjectVersionData = ProjectVersionData(),
  crossinline block: ProjectTemplateBuilder.() -> Unit
): ProjectTemplate {
  return ProjectTemplateBuilder().apply {

    // When project name is changed, change the package name accordingly
    projectName.observe { name ->
      val newPackage =
        AndroidUtils.appNameToPackageName(name.value, packageName.value)
      packageName.setValue(newPackage)
    }

    Environment.mkdirIfNotExits(Environment.PROJECTS_DIR)

    val saveLocation = stringParameter {
      name = R.string.wizard_save_location
      default = Environment.PROJECTS_DIR.absolutePath
      endIcon = { R.drawable.ic_folder }
      constraints = listOf(NONEMPTY, DIRECTORY, EXISTS)
    }

    projectName.doBeforeCreateView {
      it.setValue(getNewProjectName(saveLocation.value, projectName.value))
    }

    widgets(TextFieldWidget(projectName), TextFieldWidget(packageName),
      TextFieldWidget(saveLocation), SpinnerWidget(language),
      SpinnerWidget(minSdk), CheckBoxWidget(useKts))

    // Setup the required properties before executing the recipe
    preRecipe = {
      this@apply._executor = this

      this@apply._data = ProjectTemplateData(projectName.value,
        File(saveLocation.value, projectName.value), projectVersionData,
        language = language.value, useKts = useKts.value)

      if (data.projectDir.exists() && data.projectDir.listFiles()
          ?.isNotEmpty() == true
      ) {
        throw IllegalArgumentException("Project directory already exists")
      }

      setDefaultModuleData(
        ModuleTemplateData(":app", appName = data.name, packageName.value,
          data.moduleNameToDir(":app"), type = AndroidApp,
          language = language.value, minSdk = minSdk.value,
          useKts = data.useKts))
    }

    // After the recipe is executed, finalize the project creation
    // In this phase, we write the build scripts as they may need additional data based on the previous recipe
    // For example, writing settings.gradle[.kts] needs to know the name of the modules so that those can be includedl
    postRecipe = {
      // build.gradle[.kts]
      buildGradle()

      // settings.gradle[.kts]
      settingsGradle()

      // gradle.properties
      gradleProps()

      // gradlew
      // gradlew.bat
      // gradle/wrapper/gradle-wrapper.jar
      // gradle/wrapper/gradle-wrapper.properties
      gradleWrapper()

      // .gitignore
      gitignore()
    }

    block()

  }.build() as ProjectTemplate
}

/**
 * Create a new module project in this project.
 *
 * @param block The module configurator.
 */
inline fun baseAndroidModule(isLibrary: Boolean = false,
  crossinline block: AndroidModuleTemplateConfigurator
): ModuleTemplate {
  return AndroidModuleTemplateBuilder().apply {

    val appName = if (isLibrary) null else projectNameParameter()
    val language = projectLanguageParameter()
    val minSdk = minSdkParameter()
    val packageName = packageNameParameter()
    val useKts = useKtsParameter()

    val moduleName = stringParameter {
      name = R.string.wizard_module_name
      default = ":app"
      constraints = listOf(NONEMPTY, MODULE_NAME)
    }

    val type = enumParameter<ModuleType> {
      name = R.string.wizard_module_type
      default = AndroidLibrary
      startIcon = { R.drawable.ic_android }
      displayName = ModuleType::typeName
    }

    widgets(TextFieldWidget(moduleName))

    appName?.let {
      widgets(TextFieldWidget(it))
    }

    widgets(TextFieldWidget(packageName), SpinnerWidget(minSdk),
      SpinnerWidget(type), SpinnerWidget(language), CheckBoxWidget(useKts))

    preRecipe = commonPreRecipe {
      ModuleTemplateData(name = moduleName.value, appName = appName?.value,
        packageName = packageName.value,
        projectDir = requireProjectData().moduleNameToDir(moduleName.value),
        type = type.value, language = language.value, minSdk = minSdk.value,
        useKts = useKts.value)
    }
    postRecipe = commonPostRecipe()

    block()
  }.build() as ModuleTemplate
}

/**
 * Creates a template for a file.
 *
 * @param dir The directory in which the file will be created.
 * @param configurator The configurator to configure the template.
 * @return The [FileTemplate].
 */
inline fun <R : FileTemplateRecipeResult> baseFile(dir: File,
  crossinline configurator: FileTemplateBuilder<R>.() -> Unit
): FileTemplate<R> {
  return FileTemplateBuilder<R>(dir).apply(configurator)
    .build() as FileTemplate<R>
}