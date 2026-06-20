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
package com.example.boxcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import ortus.boxlang.runtime.android.AndroidBoxRuntime
import ortus.boxlang.runtime.android.ComposeBridge
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext

/**
 * The Compose track's thin Kotlin host.
 *
 * All UI structure and logic live in BoxLang (src/main/bx/handlers/Main.bx returns the UI
 * tree built in NativeScreen.bx). This host just bootstraps a request context and hands the
 * BoxLang-produced [ortus.boxlang.runtime.android.ui.UINode] tree to the Compose renderer.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val android = AndroidBoxRuntime.getInstance()
        val context = ScriptingRequestBoxContext(android.runtime.runtimeContext, true)

        // Invoke the BoxLang entry handler to obtain the UI tree, then render it natively.
        // A closure is passed so the renderer can re-invoke it on recomposition.
        val uiClosure = android.runtime.executeStatement(
            "() => new handlers.Main().index()",
            context
        )
        ComposeBridge.render(this, context, uiClosure)
    }
}
