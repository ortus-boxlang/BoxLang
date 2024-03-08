package ortus.boxlang.debugger;

import java.util.Optional;

import com.sun.jdi.request.StepRequest;

import ortus.boxlang.debugger.BoxLangDebugger.StackFrameTuple;

public class NextStepStrategy implements IStepStrategy {

	private StepRequest		stepRequest;
	private int				originalStackSize	= 1;
	private StackFrameTuple	originalFrame		= null;

	@Override
	public void startStepping( CachedThreadReference ref ) {
		originalStackSize	= ref.getBoxLangStackFrames().size();
		originalFrame		= ref.getBoxLangStackFrames().get( 0 );

		stepRequest			= ref.vm.eventRequestManager().createStepRequest( ref.threadReference, StepRequest.STEP_LINE, StepRequest.STEP_OVER );
		stepRequest.enable();
	}

	@Override
	public Optional<StackFrameTuple> checkStepEvent( CachedThreadReference ref ) {
		if ( ref.getBoxLangStackFrames().size() == 0 ) {
			return Optional.empty();
		}

		var topFrame = ref.getBoxLangStackFrames().get( 0 );

		if ( ref.getBoxLangStackFrames().size() < this.originalStackSize ) {
			return Optional.of( topFrame );
		}

		if ( topFrame.sourceLine() != originalFrame.sourceLine() ) {
			return Optional.of( topFrame );
		}

		return Optional.empty();
	}

	@Override
	public void dispose() {
		this.stepRequest.disable();
	}

}
