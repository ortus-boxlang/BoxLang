package ortus.boxlang.debugger;

import java.util.Optional;

import com.sun.jdi.request.StepRequest;

import ortus.boxlang.debugger.BoxLangDebugger.StackFrameTuple;

public class StepInStrategy implements IStepStrategy {

	private StepRequest		stepRequest;
	private StackFrameTuple	originalFrame	= null;

	@Override
	public void startStepping( CachedThreadReference ref ) {
		originalFrame	= ref.getBoxLangStackFrames().get( 0 );

		stepRequest		= ref.vm.eventRequestManager().createStepRequest( ref.threadReference, StepRequest.STEP_LINE, StepRequest.STEP_INTO );
		stepRequest.addClassFilter( "boxgenerated.*" );
		stepRequest.enable();
	}

	@Override
	public Optional<StackFrameTuple> checkStepEvent( CachedThreadReference ref ) {
		return IStepStrategy.basicStepBehavior( ref, originalFrame );
	}

	@Override
	public void dispose() {
		this.stepRequest.disable();
	}

}
