package ucla_sensing.com.ntp_test;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

public class ntpThread extends Thread {

    ntpThread() {}

    @Override
    public synchronized void start() {
        super.start();


    }

    public static long getNTPDate() {

        String[] hosts = new String[] { "ntp02.oal.ul.pt", "ntp04.oal.ul.pt",
                "ntp.xs4all.nl", "time.foo.com", "time.nist.gov" };

        NTPUDPClient client = new NTPUDPClient();
        // We want to timeout if a response takes longer than 5 seconds
        client.setDefaultTimeout(2000);
        for (String host : hosts) {
            try {
                InetAddress hostAddr = InetAddress.getByName(host);
                TimeInfo info = client.getTime(hostAddr);
                Date date = new Date(info.getReturnTime());
                return date.getTime();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        client.close();
        return 0;
    }
}
