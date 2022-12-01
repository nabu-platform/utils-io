# Problem

It started with a single project which suddenly would not load properly in the browser.
Firefox gave no concrete error, but chrome reported a ERR_INVALID_CHUNKED_ENCODING.
Upon studying the results further, the CSS was coming in without chunking issues but it was completely messed up, the content was entirely scrambled. Note that the first bit however, was always accurate. (read: the first TCP packet was OK, from then on it started to get messed up).

# Research

An interesting observation to make is that while the problem only occurs if SSL is enabled, SSL itself is _not_ causing any issue, meaning all the data is correctly encrypted/decrypted and the handshakes are still working etc.

Is is the application data that is getting scrambled, leading to invalid chunks (if it is chunked) or just scrambled messages in general.

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

# Chapter Two: the saga continues

The date is 2022-12-01. The level of frustration: extreme.
 
Symptoms: in say the past month or two we noticed twice (_extremely_ low frequency) that a network call would hang.
This happened to one javascript file, one css file (and yesterday) a swagger.json so it is not content related.

By "hanging", it meant if you tried to load that particular file in the browser, it would just show a "load icon" indefinitely. If you waited long enough (1+ min) in chrome, it would end with a net::ERR_INCOMPLETE_CHUNKED_ENCODING.

Note that it is very stable: as long as you don't change the original file (it seems to be tied to a _very_ specific content size or something similar), the problem will persist indefinitely (even across reboots). The first hit after reboot _might_ work which made me look at TLS 1.3 continuations but I never found anything in that direction. All other hits after reboot definitely messed it up.

This is if you requested it via the proxy, if you sshed into the server directly, the call would succeed. After long debugging sessions on the proxy that made sure the chunking was actually done correctly, I stumbled upon TLS1.3 as the culprit.

I only happened upon it by accident, because wireshark can't decode TLS1.3 easily and all my other attempts (command line tools, java clients,...) could not reproduce the error output. By switching the server to TLS1.2, it suddenly worked again, allowing me to zoom in on TLS1.3 as the culprit.

I had sadly forgotten about this file but upon checking out the code, was pointed towards it. So now that you're all caught up, let's look at the actual problem.

Sidenote: once I had determined that TLS1.3 was the problem, I could force my local java client (running the nabu http client) to use TLS1.3 and then I _could_ reproduce the error and see that the last part of the chunk was indeed missing.

## Problem

It is not entirely clear what the problem is really...But after checking out the code for a bit and making sure we are not leaving obvious data on the table (for example the flush() seems very meager but there was no remaining data in any buffers), I noticed that the wrap was really optimistic about its ability to push all applicationOut data into the network buffer.

In other words: if for some reason not all data could be wrapped from the application out into the network buffer, it is hard to see where the remaining data would be "recovered".

## Workaround

The problem yesterday came to light after a production deployment. I changed the swagger provider to include one additional (unused) method just to force the swagger.json to be slightly different. This was enough to push it over the edge and work again.

## Solution 

I could build a whole system of checks and balances to make sure the applicationOut is properly cleared and/or flushed. And I still might.
But after this 10-hour-long debug session which takes place during quite possibly the busiest time of year, I'm taking the simpler solution.

Because of the extremely low occurrence rate (3 times over as many months across _all_ our servers) and the simplicity of the fix, it seems enough.

We size the applicationIn and applicationOut buffer to be the same: the maximum size of an application packet as dictated by the ssl engine.
For incoming data this is a _must_ because the sending application might very well keep to that maximum.

However, the applicationOut is entirely under our control and we can reduce its size without impacting the application.
In retrospect, if you read through the documentation of getApplicationBufferSize() you see that it specifically states that the outbound buffer can be of any size.

So the solution (for now): divide the applicationOut buffer size by 2.