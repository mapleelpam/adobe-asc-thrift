package adobe.abc;

public interface OptimizerPlugin
{
	public void initializePlugin(adobe.abc.GlobalOptimizer caller, java.util.Vector<String> options);
	public void runPlugin(String abc_filename, adobe.abc.CallGraph call_graph);
	
}
