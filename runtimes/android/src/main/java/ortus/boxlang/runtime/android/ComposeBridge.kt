/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.android

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ortus.boxlang.runtime.android.ui.UINode
import ortus.boxlang.runtime.context.IBoxContext

/**
 * Bridges the BoxLang Compose track to a host [ComponentActivity].
 *
 * Accepts a BoxLang UI value that is either a [UINode] tree or a closure that returns one.
 * The closure form enables React-style re-rendering: a `version` state increments whenever
 * the app requests a re-render, causing the closure to be re-invoked and the tree rebuilt.
 */
object ComposeBridge {

    /** Bumped to force the UI closure to re-run and recompose. */
    @JvmStatic
    fun render(activity: ComponentActivity, context: IBoxContext, uiTree: Any?) {
        activity.setContent {
            // A trivial recomposition trigger; the app can call requestRender() to bump it.
            var version by remember { mutableStateOf(0) }
            RenderState.bind { version++ }

            // Re-evaluate the tree on each version change.
            @Suppress("UNUSED_EXPRESSION")
            version
            val node = resolveTree(context, uiTree)
            if (node != null) {
                ComposeTreeRenderer.Render(context, node)
            }
        }
    }

    private fun resolveTree(context: IBoxContext, uiTree: Any?): UINode? = when (uiTree) {
        is UINode -> uiTree
        null -> null
        // A BoxLang closure/function that returns a UINode tree.
        else -> context.invokeFunction(uiTree) as? UINode
    }

    /** Holds the active recomposition trigger so app code can request a re-render. */
    object RenderState {
        private var trigger: (() -> Unit)? = null

        fun bind(trigger: () -> Unit) {
            this.trigger = trigger
        }

        @JvmStatic
        fun requestRender() {
            trigger?.invoke()
        }
    }
}
