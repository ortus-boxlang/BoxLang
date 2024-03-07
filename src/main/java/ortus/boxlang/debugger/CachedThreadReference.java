package ortus.boxlang.debugger;

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;

import ortus.boxlang.debugger.BoxLangDebugger.StackFrameTuple;

public class CachedThreadReference {

	private ThreadReference			threadReference;
	private List<StackFrameTuple>	stackFrames	= new ArrayList<StackFrameTuple>();

	public CachedThreadReference( ThreadReference threadReference ) {
		this.threadReference = threadReference;

		this.cacheStackFrames();
	}

	public List<StackFrameTuple> getBoxLangStackFrames() {
		return this.stackFrames;
	}

	private void cacheStackFrames() {
		try {
			this.threadReference.frames()
			    .stream()
			    .filter( ( stackFrame ) -> stackFrame.location().declaringType().name().contains( "boxgenerated" ) )
			    .filter( ( stackFrame ) -> !stackFrame.location().method().name().contains( "dereferenceAndInvoke" ) )
			    .forEach( ( sf ) -> {
				    try {

					    this.stackFrames
					        .add( new StackFrameTuple( sf, sf.location(), sf.hashCode(), sf.getValues( sf.visibleVariables() ), this.threadReference ) );
				    } catch ( AbsentInformationException e ) {
					    // TODO handle exception
					    e.printStackTrace();
				    }
			    } );
		} catch ( IncompatibleThreadStateException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
