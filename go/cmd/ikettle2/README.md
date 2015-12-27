Command line shell for Smarter iKettle 2.0
==========================================

A command line shell for the [Smarter iKettle 2.0](http://smarter.am/ikettle-2.0/)
as a replacement for the terrible Android app supplied!

**Note** iKettle must be pre-configured using the Android app - good luck!

Getting started:

1. `go get github.com/ian-kent/ikettle2/go/cmd/ikettle2`
2. Run `ikettle2`

Examples:

```
connect 192.168.1.100
disconnect
connect
on
on 80
on 100
off
calibrate
```

Settings are auto-saved to `.ikettle` using JSON.

* `connect` requires a host/ip on first use, but saves it for subsequent sessions
* `on` can specify a temperature, but defaults to 100
* `calibrate` calibrates the water level sensor

**Note** water levels reported by the shell are wrong - WIP!

### Configuring the iKettle

After a lot of trial and error, these steps worked semi-reliably:

1. Use the Android app - it sucks, but it (sort of) works for this bit
2. Be on a WiFi connection with *working* internet - **don't change this yet!**
3. Start the Android app, choose iKettle and 'Setup Home Network'
4. Click Next until it tells you to change WiFi network - then switch to
   the `iKettle 2.0:fd` network
5. Click Next and configure the WiFi network

If you see a `Success` toast, it's worked - but you probably still can't
connect to it (but the `iKettle 2.0:fd` network should disappear).

Troubleshooting is easy: if anything fails, force close the Android app (from
the Android settings Apps menu, seriously), switch back to your main WiFi
network, and start over again.

### Licence

Copyright ©‎ 2015, Ian Kent (http://iankent.uk).

Released under MIT license, see [LICENSE](LICENSE.md) for details.
