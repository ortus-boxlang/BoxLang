package ortus.boxlang.debugger;

import java.util.Optional;

import ortus.boxlang.debugger.BoxLangDebugger.StackFrameTuple;

public interface IStepStrategy {

	public void startStepping( CachedThreadReference ref );

	public Optional<StackFrameTuple> checkStepEvent( CachedThreadReference ref );

	public void dispose();
}
