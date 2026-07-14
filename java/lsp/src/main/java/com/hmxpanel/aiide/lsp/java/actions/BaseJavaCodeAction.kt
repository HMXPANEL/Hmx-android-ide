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

package com.hmxpanel.aiide.lsp.java.actions

import android.content.Context
import android.graphics.drawable.Drawable
import com.hmxpanel.aiide.actions.ActionData
import com.hmxpanel.aiide.actions.ActionItem
import com.hmxpanel.aiide.actions.EditorActionItem
import com.hmxpanel.aiide.actions.hasRequiredData
import com.hmxpanel.aiide.actions.markInvisible
import com.hmxpanel.aiide.actions.requireFile
import com.hmxpanel.aiide.lsp.api.ILanguageClient
import com.hmxpanel.aiide.lsp.api.ILanguageServerRegistry
import com.hmxpanel.aiide.lsp.java.JavaCompilerProvider
import com.hmxpanel.aiide.lsp.java.JavaLanguageServer
import com.hmxpanel.aiide.lsp.java.R
import com.hmxpanel.aiide.lsp.java.compiler.JavaCompilerService
import com.hmxpanel.aiide.lsp.java.rewrite.Rewrite
import com.hmxpanel.aiide.projects.IProjectManager
import com.hmxpanel.aiide.utils.DocumentUtils
import com.hmxpanel.aiide.utils.ILogger
import com.hmxpanel.aiide.utils.flashError
import java.io.File

/**
 * Base class for java code actions
 *
 * @author Akash Yadav
 */
abstract class BaseJavaCodeAction : EditorActionItem {

  override var visible: Boolean = true
  override var enabled: Boolean = true
  override var icon: Drawable? = null
  override var requiresUIThread: Boolean = false
  override var location: ActionItem.Location = ActionItem.Location.EDITOR_CODE_ACTIONS

  protected abstract val titleTextRes: Int

  override fun prepare(data: ActionData) {
    super.prepare(data)
    if (
      !data.hasRequiredData(Context::class.java, JavaLanguageServer::class.java, File::class.java)
    ) {
      markInvisible()
      return
    }

    if (titleTextRes != -1) {
      label = data[Context::class.java]!!.getString(titleTextRes)
    }

    val file = data.requireFile()
    visible = DocumentUtils.isJavaFile(file.toPath())
    enabled = visible
  }

  fun performCodeAction(data: ActionData, result: Rewrite) {
    val compiler = data.requireCompiler()

    val actions =
      try {
        result.asCodeActions(compiler, label)
      } catch (e: Exception) {
        flashError(e.cause?.message ?: e.message)
        ILogger.ROOT.error(e.cause?.message ?: e.message, e)
        return
      }

    if (actions == null) {
      onPerformCodeActionFailed(data)
      return
    }

    data.getLanguageClient()?.performCodeAction(actions)
  }

  protected open fun onPerformCodeActionFailed(data: ActionData) {
    flashError(R.string.msg_codeaction_failed)
  }

  protected fun ActionData.requireLanguageServer(): JavaLanguageServer {
    return ILanguageServerRegistry.getDefault().getServer(JavaLanguageServer.SERVER_ID)
        as JavaLanguageServer
  }

  protected fun ActionData.getLanguageClient(): ILanguageClient? {
    return requireLanguageServer().client
  }

  protected fun ActionData.requireCompiler(): JavaCompilerService {
    val module =
      IProjectManager.getInstance().getWorkspace()?.findModuleForFile(requireFile(), false)
    requireNotNull(module) {
      "Cannot get compiler instance. Unable to find module for file: ${requireFile().name}"
    }
    return JavaCompilerProvider.get(module)
  }
}
