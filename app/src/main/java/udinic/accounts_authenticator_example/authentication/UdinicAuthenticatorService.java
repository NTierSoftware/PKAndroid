package udinic.accounts_authenticator_example.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.slf4j.*;


/**
 * Created with IntelliJ IDEA.
 * User: Udini
 * Date: 19/03/13
 * Time: 19:10
 */
public class UdinicAuthenticatorService extends Service {
static private final Logger mLog = LoggerFactory.getLogger( UdinicAuthenticatorService.class );

    @Override
    public IBinder onBind(Intent intent) {
		mLog.debug("UdinicAuthenticatorService:\t onBind");

        UdinicAuthenticator authenticator = new UdinicAuthenticator(this);
        return authenticator.getIBinder();
    }
}
