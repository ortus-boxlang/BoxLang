package ortus.boxlang.debugger;

import com.sun.jdi.VirtualMachine;

public interface IVMInitializationStrategy {

	public VirtualMachine initialize() throws Exception;
}
