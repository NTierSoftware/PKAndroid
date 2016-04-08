//See https://northconcepts.com/blog/2013/01/18/6-tips-to-improve-your-exception-handling/
package org.peacekeeper.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//http://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html
//" If a client cannot do anything to recover from the exception, make it an unchecked exception."
//pkException is an application wide unchecked exception
public class pkException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private pkErrCode mErrCode;

    private static final Logger	mLog	= LoggerFactory.getLogger( pkException.class );

    public static pkException wrap(Throwable exception, pkErrCode errorCode) {
/*    	Long, redundant stack traces help no one. 
    	Even worse, they waste your time and resources.  
    	When rethrowing exceptions, 
    	call a static wrap method instead of the exception�s constructor . 
    	The wrap method will be responsible for deciding when 
    	to nest exceptions and when to just return the original instance.
*/    			
        if (exception instanceof pkException) {
            pkException ge = (pkException)exception;
        	if (errorCode != null && errorCode != ge.getErrorCode()) {
                return new pkException(exception.getMessage(), exception, errorCode);
			}
			return ge;
        }
		return new pkException(exception.getMessage(), exception, errorCode);
    }
    
    public static pkException wrap(Throwable exception) { return wrap(exception, null); }
    
    
    private final Map<String,Object> properties = new TreeMap<String,Object>();
    
    
    public pkException(pkErrCode errorCode) { mErrCode = errorCode; }

	public pkException(String message, pkErrCode errorCode) {
		super(message);
		this.mErrCode = errorCode;
	}

	public pkException(Throwable cause, pkErrCode errorCode) {
		super(cause);
		this.mErrCode = errorCode;
	}

	public pkException(String message, Throwable cause, pkErrCode errorCode) {
		super(message, cause);
		this.mErrCode = errorCode;
	}
	
	public pkErrCode getErrorCode() { return mErrCode; }
	
	public pkException setErrorCode(pkErrCode errorCode) {
        this.mErrCode = errorCode;
        return this;
    }
	
	public Map<String, Object> getProperties() { return properties; }
	
    //@SuppressWarnings("unchecked")
	public <T> T get(String name) { return (T)properties.get(name); }
	
    public pkException set(String name, Object value) {
        properties.put(name, value);
        return this;
    }
    
    @Override
	public void printStackTrace(PrintStream s) {
        synchronized (s) {
            printStackTrace(new PrintWriter(s));
        }
    }

    @Override
	public void printStackTrace(PrintWriter s) { 
        synchronized (s) {
            s.println("\t-------------------------------");
            if (mErrCode != null) {
	        	s.println("\t" + mErrCode + ":" + mErrCode.getClass().getName()); 
			}
            s.println( this );
            
            s.println("\t-------------------------------");
            StackTraceElement[] trace = getStackTrace();
            for (int i=0; i < trace.length; i++)
                s.println("\tat " + trace[i]);

            Throwable ourCause = getCause();
            if (ourCause != null) { ourCause.printStackTrace(s); }
            s.flush();
        }
    }

    @Override public String getMessage() { return toString();}
    @Override 
    public String toString()   {
    	StringBuilder retVal = new StringBuilder()
    									.append("\n")
    									.append(mErrCode.mErrMsg);
    	
        for (String key : properties.keySet()) {
        	retVal.append("\n:\t" + key + "=[" )
        			.append(properties.get(key) + "]");
        }
	return retVal.append("\n").toString() ;
    }
    
    //public void log(){ log.error(toString()); }
}//class pkException

