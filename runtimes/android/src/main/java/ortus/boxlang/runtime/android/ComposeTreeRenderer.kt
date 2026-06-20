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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ortus.boxlang.runtime.android.ui.UINode
import ortus.boxlang.runtime.context.IBoxContext
import ortus.boxlang.runtime.scopes.Key

/**
 * Walks a BoxLang [UINode] tree and emits genuine Jetpack Compose widgets.
 *
 * Node `type` names map 1:1 to Compose composables; event handlers (BoxLang closures stored
 * on the node) are invoked through the runtime via [IBoxContext.invokeFunction]. This is the
 * single real `@Composable` host that backs the otherwise-pure-BoxLang Track 1 UI tree.
 */
object ComposeTreeRenderer {

    @Composable
    fun Render(context: IBoxContext, node: UINode) {
        when (node.type) {
            "Column" -> Column { RenderChildren(context, node) }
            "Row" -> Row { RenderChildren(context, node) }
            "Box" -> Box { RenderChildren(context, node) }
            "Text" -> Text(text = stringProp(node, "text"))
            "Spacer" -> Spacer(
                modifier = Modifier
                    .height(intProp(node, "size").dp)
                    .width(intProp(node, "size").dp)
            )
            "Button" -> Button(onClick = { fire(context, node, "onClick") }) {
                Text(text = stringProp(node, "label"))
            }
            "TextField" -> OutlinedTextField(
                value = stringProp(node, "value"),
                onValueChange = { newValue -> fire(context, node, "onChange", newValue) }
            )
            else -> Text(text = "Unknown node: ${node.type}")
        }
    }

    @Composable
    private fun RenderChildren(context: IBoxContext, node: UINode) {
        for (child in node.children) {
            Render(context, child)
        }
    }

    /**
     * Invoke a BoxLang closure registered as an event handler on the node.
     */
    private fun fire(context: IBoxContext, node: UINode, event: String, vararg args: Any?) {
        val handler = node.events[Key.of(event)] ?: return
        context.invokeFunction(handler, args.toList().toTypedArray())
    }

    private fun stringProp(node: UINode, name: String): String =
        node.getProp(name)?.toString() ?: ""

    private fun intProp(node: UINode, name: String): Int =
        (node.getProp(name) as? Number)?.toInt() ?: 0
}
