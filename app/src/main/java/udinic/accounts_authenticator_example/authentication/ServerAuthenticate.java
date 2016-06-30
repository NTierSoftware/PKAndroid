package udinic.accounts_authenticator_example.authentication;

/**
 * User: udinic
 * Date: 3/27/13
 * Time: 2:35 AM
 */
public interface ServerAuthenticate {
    String userSignUp( final String name,
                       final String email,
                       final String pass,
                       String authType )
    throws Exception;

    String userSignIn( final String user,
                       final String pass,
                       String authType )
    throws Exception;
}
