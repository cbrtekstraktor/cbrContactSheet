package logger;


public class logLiason {
	private long LogLiasonId = System.nanoTime();
	
	
	public logLiason(long l)
	{
	 LogLiasonId =l;
	
	}
	
	public void write( String iclassName , int ilogLevel , String ilogMsg )
	{
		System.out.println(ilogMsg);
	}

	public void close()
	{
		
	}
}
