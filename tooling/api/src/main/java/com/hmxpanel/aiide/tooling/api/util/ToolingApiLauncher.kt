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
package com.hmxpanel.aiide.tooling.api.util

import com.google.gson.GsonBuilder
import com.hmxpanel.aiide.builder.model.DefaultJavaCompileOptions
import com.hmxpanel.aiide.builder.model.IJavaCompilerSettings
import com.hmxpanel.aiide.tooling.api.IProject
import com.hmxpanel.aiide.tooling.api.IToolingApiClient
import com.hmxpanel.aiide.tooling.api.IToolingApiServer
import com.hmxpanel.aiide.tooling.api.models.AndroidProjectMetadata
import com.hmxpanel.aiide.tooling.api.models.AndroidVariantMetadata
import com.hmxpanel.aiide.tooling.api.models.BasicAndroidVariantMetadata
import com.hmxpanel.aiide.tooling.api.models.BasicProjectMetadata
import com.hmxpanel.aiide.tooling.api.models.GradleTask
import com.hmxpanel.aiide.tooling.api.models.JavaModuleCompilerSettings
import com.hmxpanel.aiide.tooling.api.models.JavaModuleDependency
import com.hmxpanel.aiide.tooling.api.models.JavaModuleExternalDependency
import com.hmxpanel.aiide.tooling.api.models.JavaModuleProjectDependency
import com.hmxpanel.aiide.tooling.api.models.JavaProjectMetadata
import com.hmxpanel.aiide.tooling.api.models.Launchable
import com.hmxpanel.aiide.tooling.api.models.ProjectMetadata
import com.hmxpanel.aiide.tooling.events.OperationDescriptor
import com.hmxpanel.aiide.tooling.events.OperationResult
import com.hmxpanel.aiide.tooling.events.ProgressEvent
import com.hmxpanel.aiide.tooling.events.StatusEvent
import com.hmxpanel.aiide.tooling.events.configuration.ProjectConfigurationFinishEvent
import com.hmxpanel.aiide.tooling.events.configuration.ProjectConfigurationOperationDescriptor
import com.hmxpanel.aiide.tooling.events.configuration.ProjectConfigurationOperationResult
import com.hmxpanel.aiide.tooling.events.configuration.ProjectConfigurationProgressEvent
import com.hmxpanel.aiide.tooling.events.configuration.ProjectConfigurationStartEvent
import com.hmxpanel.aiide.tooling.events.download.FileDownloadFinishEvent
import com.hmxpanel.aiide.tooling.events.download.FileDownloadOperationDescriptor
import com.hmxpanel.aiide.tooling.events.download.FileDownloadProgressEvent
import com.hmxpanel.aiide.tooling.events.download.FileDownloadResult
import com.hmxpanel.aiide.tooling.events.download.FileDownloadStartEvent
import com.hmxpanel.aiide.tooling.events.internal.DefaultFinishEvent
import com.hmxpanel.aiide.tooling.events.internal.DefaultOperationDescriptor
import com.hmxpanel.aiide.tooling.events.internal.DefaultOperationResult
import com.hmxpanel.aiide.tooling.events.internal.DefaultProgressEvent
import com.hmxpanel.aiide.tooling.events.internal.DefaultStartEvent
import com.hmxpanel.aiide.tooling.events.task.TaskExecutionResult
import com.hmxpanel.aiide.tooling.events.task.TaskFailureResult
import com.hmxpanel.aiide.tooling.events.task.TaskFinishEvent
import com.hmxpanel.aiide.tooling.events.task.TaskOperationDescriptor
import com.hmxpanel.aiide.tooling.events.task.TaskOperationResult
import com.hmxpanel.aiide.tooling.events.task.TaskProgressEvent
import com.hmxpanel.aiide.tooling.events.task.TaskSkippedResult
import com.hmxpanel.aiide.tooling.events.task.TaskStartEvent
import com.hmxpanel.aiide.tooling.events.task.TaskSuccessResult
import com.hmxpanel.aiide.tooling.events.test.TestFinishEvent
import com.hmxpanel.aiide.tooling.events.test.TestOperationDescriptor
import com.hmxpanel.aiide.tooling.events.test.TestOperationResult
import com.hmxpanel.aiide.tooling.events.test.TestProgressEvent
import com.hmxpanel.aiide.tooling.events.test.TestStartEvent
import com.hmxpanel.aiide.tooling.events.transform.TransformFinishEvent
import com.hmxpanel.aiide.tooling.events.transform.TransformOperationDescriptor
import com.hmxpanel.aiide.tooling.events.transform.TransformProgressEvent
import com.hmxpanel.aiide.tooling.events.transform.TransformStartEvent
import com.hmxpanel.aiide.tooling.events.work.WorkItemFinishEvent
import com.hmxpanel.aiide.tooling.events.work.WorkItemOperationDescriptor
import com.hmxpanel.aiide.tooling.events.work.WorkItemOperationResult
import com.hmxpanel.aiide.tooling.events.work.WorkItemProgressEvent
import com.hmxpanel.aiide.tooling.events.work.WorkItemStartEvent
import org.eclipse.lsp4j.jsonrpc.Launcher
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

/**
 * Utility class for launching [IToolingApiClient] and [IToolingApiServer].
 *
 * @author Akash Yadav
 */
object ToolingApiLauncher {

  fun <T> createIOLauncher(
    local: Any?, remote: Class<T>?, `in`: InputStream?, out: OutputStream?): Launcher<T> {
    return Launcher.Builder<T>()
      .setInput(`in`)
      .setOutput(out)
      .setLocalService(local)
      .setRemoteInterface(remote)
      .configureGson { configureGson(it) }
      .create()
  }

  @JvmStatic
  fun configureGson(builder: GsonBuilder) {
    builder.registerTypeAdapter(File::class.java, FileTypeAdapter())

    // some methods return BasicProjectMetadata while some return ProjectMetadata
    // so we need to register type adapter for both of them
    builder.runtimeTypeAdapter(
      BasicProjectMetadata::class.java,
      ProjectMetadata::class.java,
      AndroidProjectMetadata::class.java,
      JavaProjectMetadata::class.java
    )
    builder.runtimeTypeAdapter(
      ProjectMetadata::class.java,
      AndroidProjectMetadata::class.java,
      JavaProjectMetadata::class.java
    )
    builder.runtimeTypeAdapter(
      BasicAndroidVariantMetadata::class.java,
      AndroidVariantMetadata::class.java
    )
    builder.runtimeTypeAdapter(
      JavaModuleDependency::class.java,
      JavaModuleExternalDependency::class.java,
      JavaModuleProjectDependency::class.java
    )
    builder.runtimeTypeAdapter(
      IJavaCompilerSettings::class.java,
      DefaultJavaCompileOptions::class.java,
      JavaModuleCompilerSettings::class.java
    )
    builder.runtimeTypeAdapter(
      Launchable::class.java,
      GradleTask::class.java
    )
    builder.runtimeTypeAdapter(
      ProgressEvent::class.java,
      ProjectConfigurationProgressEvent::class.java,
      ProjectConfigurationStartEvent::class.java,
      ProjectConfigurationFinishEvent::class.java,

      FileDownloadProgressEvent::class.java,
      FileDownloadStartEvent::class.java,
      FileDownloadFinishEvent::class.java,

      TaskProgressEvent::class.java,
      TaskStartEvent::class.java,
      TaskFinishEvent::class.java,

      TestProgressEvent::class.java,
      TestStartEvent::class.java,
      TestFinishEvent::class.java,

      TransformProgressEvent::class.java,
      TransformStartEvent::class.java,
      TransformFinishEvent::class.java,

      WorkItemProgressEvent::class.java,
      WorkItemStartEvent::class.java,
      WorkItemFinishEvent::class.java,

      DefaultProgressEvent::class.java,
      DefaultStartEvent::class.java,
      DefaultFinishEvent::class.java,

      StatusEvent::class.java
    )
    builder.runtimeTypeAdapter(
      OperationDescriptor::class.java,
      ProjectConfigurationOperationDescriptor::class.java,
      FileDownloadOperationDescriptor::class.java,
      TaskOperationDescriptor::class.java,
      TestOperationDescriptor::class.java,
      TransformOperationDescriptor::class.java,
      WorkItemOperationDescriptor::class.java,
      DefaultOperationDescriptor::class.java
    )
    builder.runtimeTypeAdapter(
      OperationResult::class.java,
      ProjectConfigurationOperationResult::class.java,
      FileDownloadResult::class.java,
      TaskOperationResult::class.java,
      TestOperationResult::class.java,
      WorkItemOperationResult::class.java,
      DefaultOperationResult::class.java
    )
    builder.runtimeTypeAdapter(
      TaskOperationResult::class.java,
      TaskFailureResult::class.java,
      TaskSkippedResult::class.java,
      TaskExecutionResult::class.java,
      TaskSuccessResult::class.java
    )
  }

  private fun <T> GsonBuilder.runtimeTypeAdapter(baseClass: Class<T>,
    vararg subtypes: Class<out T>) {
    registerTypeAdapterFactory(
      RuntimeTypeAdapterFactory.of(baseClass, "gsonType", true)
        .registerSubtype(baseClass, baseClass.name).also { factory ->
          subtypes.forEach { subtype ->
            factory.registerSubtype(subtype, subtype.name)
          }
        }
    )
  }

  fun newClientLauncher(
    client: IToolingApiClient, `in`: InputStream?, out: OutputStream?): Launcher<Any> {
    return newIoLauncher(arrayOf(client), arrayOf(
      IToolingApiServer::class.java, IProject::class.java), `in`, out)
  }

  fun newIoLauncher(
    locals: Array<Any>, remotes: Array<Class<*>?>, `in`: InputStream?,
    out: OutputStream?): Launcher<Any> {
    return Launcher.Builder<Any>()
      .setInput(`in`)
      .setOutput(out)
      .setExecutorService(Executors.newCachedThreadPool())
      .setLocalServices(listOf(*locals))
      .setRemoteInterfaces(listOf(*remotes))
      .configureGson { configureGson(it) }
      .setClassLoader(locals[0].javaClass.classLoader)
      .create()
  }

  @JvmStatic
  fun newServerLauncher(
    server: IToolingApiServer, project: IProject, `in`: InputStream?,
    out: OutputStream?): Launcher<Any> {
    return newIoLauncher(arrayOf(server, project), arrayOf(
      IToolingApiClient::class.java), `in`, out)
  }
}