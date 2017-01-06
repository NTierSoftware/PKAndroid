package udinic.accounts_authenticator_example.authentication;
public interface ServerAuthenticate {
    String userSignUp( final String name,
                       final String email,
//                       final String pass,
                       final String ZIP,
                       String authType )
    throws Exception;

    String userSignIn( final String user,
                       final String pass,
                       String authType )
    throws Exception;
}
