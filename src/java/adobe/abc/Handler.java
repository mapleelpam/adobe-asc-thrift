package adobe.abc;

public class Handler
{
	Typeref type;
	Block entry;
	Typeref activation;
	Name name;
	
	public String toString()
	{
		return "catch "+type;
	}
}