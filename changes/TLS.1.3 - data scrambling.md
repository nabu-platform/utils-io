# Problem

It started with a single project which suddenly would not load properly in the browser.
Firefox gave no concrete error, but chrome reported a ERR_INVALID_CHUNKED_ENCODING.
Upon studying the results further, the CSS was coming in without chunking issues but it was completely messed up, the content was entirely scrambled. Note that the first bit however, was always accurate. (read: the first TCP packet was OK, from then on it started to get messed up).

# Research

After checking a lot of other possible causes, it appeared that the java version being used was one managed by the debian server.
This java version had been automatically updated to 8.275.
Without checking further (deadlines), I rolled back to an older version and it worked again.

A few weeks later the same started occuring on a particular project (in qlty) which was behind the proxy.
The server itself was an OK version but one of the two qlty servers had the automanaged JDK which had been updated.

After some testing, it appeared that the problems only occur if SSL is enabled.

By manually trying versions starting from the one that fails and going back in time, it appears 8.265 was still working, but it broke in 8.272.
Checking the release notes, it appears that the only feature they added was a backport of TLS 1.3 from jdk 11 and because we know the issue was related to security, this was an immediate red flag.

TLS 1.3 has been active in jdk 11 and upwards since at least 2018, so I tested with java 11 and 14 and confirmed that in production mode (to get chunking) and with SSL enabled, the problem occured there as well.
**Note**: in retrospect, because we now know the cause, it is likely that the problem would also occur without chunking, as long as the message was long enough.

Release notes: https://mail.openjdk.java.net/pipermail/jdk8u-dev/2020-October/012817.html
The original bug tracker entry for the backport: https://bugs.openjdk.java.net/browse/JDK-8248721

By default, the server will use TLS 1.3. You can check the chosen TLS version in chrome -> developer tools -> security tab.

# Workaround

A workaround is to disable TLS 1.3 in file jdk-14.0.2/conf/security/java.security:


```
# add the TLSv1.3 entry in the already existing line:
jdk.tls.disabledAlgorithms=SSLv3, TLSv1.3, RC4, DES, MD5withRSA, DH keySize < 1024, \
    EC keySize < 224, 3DES_EDE_CBC, anon, NULL, \
    include jdk.disabled.namedCurves
```

A particular post suggested enabling ``-Djdk.tls.acknowledgeCloseNotify=true``
But this does not work, it changes the output a little bit (you get a little bit more javascript before it fails), but not only does it still fail, the additional javascript is (like the css) out of order.

You can debug the SSL a bit using the system property: javax.net.debug

Potentially interesting links:

- https://bensmyth.com/files/Smyth19-TLS-tutorial.pdf

# Solution

The application buffer that was being used to store application data before it is encrypted was not getting handled correctly.
Not only was it shared for input and output, it was also getting cleared, even if it could not be encrypted entirely.

It is not entirely clear why this never posed a problem until TLS 1.3, but it is nonetheless solved.

Note that the application in should not suffer the same problem as its contents are always written entirely to a buffer if necessary.

# Potential Future Problems

If requests start to hang in TLS 1.3, there might be another issue at play that I found during research for this issue: https://stackoverflow.com/questions/54687831/changes-in-sslengine-usage-when-going-up-to-tlsv1-3
If however, we explicitly close the inbound we get an error in the javax.net.debug indicating that it should come from the client? Because the core issue seems to be resolved, we don't "fix" this now until it poses a problem (if ever).

In the release notes of the backport it is mentioned that server SSL will default to TLS 1.3 (causing the issues seen here) but client SSL will default to TLS 1.2 still.
It is possible that other issues might occur in the http clients if we start using TLS 1.3 there. 
