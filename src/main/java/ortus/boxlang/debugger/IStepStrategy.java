package ortus.boxlang.debugger;

import java.util.Optional;

import ortus.boxlang.debugger.BoxLangDebugger.StackFrameTuple;

public interface IStepStrategy {

	public static Optional<StackFrameTuple> basicStepBehavior( CachedThreadReference ref, StackFrameTuple originalFrame ) {
		if ( ref.getBoxLangStackFrames().size() == 0 ) {
			return Optional.empty();
		}

		var topFrame = ref.getBoxLangStackFrames().get( 0 );

		if ( topFrame.sourceLine() == -1 || topFrame.sourceLine() == originalFrame.sourceLine() ) {
			return Optional.empty();
		}

		return Optional.of( topFrame );
	}

	public void startStepping( CachedThreadReference ref );

	public Optional<StackFrameTuple> checkStepEvent( CachedThreadReference ref );

	public void dispose();
}
