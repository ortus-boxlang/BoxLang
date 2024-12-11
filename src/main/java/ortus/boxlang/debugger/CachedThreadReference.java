package ortus.boxlang.debugger;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

import ortus.boxlang.debugger.BoxLangDebugger.StackFrameTuple;
import ortus.boxlang.runtime.BoxRuntime;

public class CachedThreadReference {

	private BoxLangDebugger			debugger;
	private Logger					logger;
	public final ThreadReference	threadReference;
	public final VirtualMachine		vm;
	private List<StackFrameTuple>	stackFrames	= new ArrayList<StackFrameTuple>();

	public CachedThreadReference( BoxLangDebugger debugger, ThreadReference threadReference ) {
		this.logger				= LoggerFactory.getLogger( BoxRuntime.class );
		this.threadReference	= threadReference;
		this.vm					= threadReference.virtualMachine();
		this.debugger			= debugger;

		this.cacheStackFrames();
	}

	public List<StackFrameTuple> getBoxLangStackFrames() {
		return this.stackFrames;
	}

	private void cacheStackFrames() {
		boolean wasPaused = false;
		try {

			if ( !threadReference.isSuspended() ) {
				wasPaused = true;
				this.threadReference.suspend();
			}

			this.threadReference.frames()
			    .stream()
			    .filter( ( stackFrame ) -> {
				    return stackFrame.location().declaringType().name().contains( "boxgenerated" );
			    } )
			    .filter( ( stackFrame ) -> {
				    return !stackFrame.location().method().name().contains( "dereferenceAndInvoke" );
			    } )
			    .forEach( ( sf ) -> {
				    try {
					    // sf.getValues( sf.visibleVariables()
					    this.stackFrames
					        .add(
					            new StackFrameTuple( debugger, sf, sf.location(), sf.hashCode(), sf.getValues( sf.visibleVariables() ),
					                this.threadReference ) );
				    } catch ( AbsentInformationException e ) {
					    logger.info( "Unable to gather stack frames information for {}", sf.toString() );
				    }
			    } );

		} catch ( IncompatibleThreadStateException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if ( wasPaused ) {
				this.threadReference.resume();
			}
		}
	}
}
