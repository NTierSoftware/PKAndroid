package udinic.accounts_authenticator_example.authentication;
/** User: Udini
 * Date: 20/03/13
 * Time: 18:11
 */

public class AccountGeneral{
    public static final String ACCOUNT_NAME = "Udinic"

//Auth token types
	, AUTHTOKEN_TYPE_READ_ONLY = "Read only"
	, AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to an Udinic account"
	, AUTHTOKEN_TYPE_FULL_ACCESS = "Full access"
	, AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to an Udinic account"
	//, ACCOUNT_TYPE = "com.udinic.auth_example"
	;

//public static final ServerAuthenticate sServerAuthenticate = new ParseComServerAuthenticate();
//public static final ServerAuthenticate sServerAuthenticate = new pkServerAuthenticate();
public static final pkServerAuthenticate sServerAuthenticate = new pkServerAuthenticate();
}//class AccountGeneral
