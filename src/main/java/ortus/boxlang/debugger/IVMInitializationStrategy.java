package ortus.boxlang.debugger;

import com.sun.jdi.VirtualMachine;

public interface IVMInitializationStrategy {

	public VirtualMachine initialize() throws Exception;

	public void disconnect( BoxLangDebugger debugger );

	public void terminate( BoxLangDebugger debugger );
}
