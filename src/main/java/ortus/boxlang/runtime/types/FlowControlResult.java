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
package ortus.boxlang.runtime.types;

/**
 * Sentinel class for method splitting flow control.
 * When methods are split due to JVM size limits, sub-methods return this
 * to signal flow control (return/break/continue) that needs to propagate
 * to the parent method.
 *
 * @param resultType  The type of flow control (NORMAL, RETURN, BREAK, CONTINUE)
 * @param returnValue The return value (for RETURN) or null
 * @param label       The label for labeled break/continue, or null
 */
public record FlowControlResult( int resultType, Object returnValue, String label ) {

	/**
	 * Flow control type constants
	 */
	public static final int NORMAL = 0;
	public static final int RETURN = 1;
	public static final int BREAK = 2;
	public static final int CONTINUE = 3;

	/**
	 * Singleton for normal completion (no flow control escape)
	 */
	public static final FlowControlResult NORMAL_RESULT = new FlowControlResult( NORMAL, null, null );

	/**
	 * --------------------------------------------------------------------------
	 * Static Factory Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a result for normal completion with a value
	 *
	 * @param value The result value
	 *
	 * @return FlowControlResult indicating normal completion
	 */
	public static FlowControlResult ofNormal( Object value ) {
		return new FlowControlResult( NORMAL, value, null );
	}

	/**
	 * Create a result for break flow control
	 *
	 * @param label The break label, or null for unlabeled break
	 *
	 * @return FlowControlResult indicating break
	 */
	public static FlowControlResult ofBreak( String label ) {
		return new FlowControlResult( BREAK, null, label );
	}

	/**
	 * Create a result for continue flow control
	 *
	 * @param label The continue label, or null for unlabeled continue
	 *
	 * @return FlowControlResult indicating continue
	 */
	public static FlowControlResult ofContinue( String label ) {
		return new FlowControlResult( CONTINUE, null, label );
	}

	/**
	 * Create a result for return flow control
	 *
	 * @param returnValue The value being returned
	 *
	 * @return FlowControlResult indicating return
	 */
	public static FlowControlResult ofReturn( Object returnValue ) {
		return new FlowControlResult( RETURN, returnValue, null );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Instance Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Check if this is a normal completion (no flow control escape)
	 *
	 * @return true if normal completion
	 */
	public boolean isNormal() {
		return this.resultType == NORMAL;
	}

	/**
	 * Check if this is a break with optional label matching
	 *
	 * @param targetLabel The label to match, or null to match any break
	 *
	 * @return true if this is a break matching the label
	 */
	public boolean isBreak( String targetLabel ) {
		return this.resultType == BREAK && ( this.label == null || this.label.equals( targetLabel ) );
	}

	/**
	 * Check if this is a continue with optional label matching
	 *
	 * @param targetLabel The label to match, or null to match any continue
	 *
	 * @return true if this is a continue matching the label
	 */
	public boolean isContinue( String targetLabel ) {
		return this.resultType == CONTINUE && ( this.label == null || this.label.equals( targetLabel ) );
	}

	/**
	 * Check if this is any break
	 *
	 * @return true if break
	 */
	public boolean isBreak() {
		return this.resultType == BREAK;
	}

	/**
	 * Check if this is any continue
	 *
	 * @return true if continue
	 */
	public boolean isContinue() {
		return this.resultType == CONTINUE;
	}

	/**
	 * Check if this is a return
	 *
	 * @return true if return
	 */
	public boolean isReturn() {
		return this.resultType == RETURN;
	}

	/**
	 * Check if this represents any early exit (not normal completion)
	 *
	 * @return true if break, continue, or return
	 */
	public boolean isEarlyExit() {
		return isBreak() || isContinue() || isReturn();
	}

	/**
	 * Get the unwrapped value for normal completion
	 *
	 * @return The return value, or null
	 */
	public Object getValue() {
		return this.returnValue;
	}

}
