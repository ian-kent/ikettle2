Smarter iKettle 2.0 protocol
============================

Smarter iKettle 2.0 uses a binary protocol, either via UDP or TCP on port 2081.

Messages (commands and responses) use the syntax: `<id> <args> <terminator>`

The terminator is always byte `126`, or `~` in ASCII.

Arguments use this syntax:

* `<arg>` is a single mandatory byte
* `<[arg]>` is a single optional byte
* `<arg>{0,32}` is mandatory, between 0 and 32 bytes

Everything else, including spurious `}` characters, are ASCII literals.

### Commands

| Command | Description            | Args
| ------- | ---------------------- | ----
| 5       | Set network SSID       | `<ssid>{0,32}`
| 7       | Set wifi password      | `<password>{0,32}`
| 12      | Complete wifi setup    |
| 13      | List wifi networks     |
| 21      | Turn on kettle         | `<[temp]> <[unknown]>`
| 22      | Turn off kettle        |
| 44      | Calibrate water sensor |
| 65      | Schedule request       | (unknown)
| 100     | Device info            |
| 109     | Firmware update        |

### Responses

| Response | Description                  | Args
| -------- | ---------------------------- | ----
| 3        | Command acknowledged         |
| 14       | List of wifi networks        | `<ssid>{0,32},-<db>{2}}`
| 20       | Kettle status                | `<flag> <temp> <wlevel1><wlevel2> <unknown>`
| 45       | Calibrate completed response | `<unknown> <unknown>`
| 101      | Device info response         | `<deviceType> <sdkVersion>`

#### Wifi network list

Appears to be in form of: `SSID,-db}`

`-db` is the signal strength in dBm format.

Examples:

* `MyWifi,-56}`
* `MyWifi,-56}OtherWifi,-82}`

### Useful info

* `<wlevel1>` appears to be:
    * `7` when the kettle is **off** the base
    * `8` when the kettle is **on** the base
* sending command `12` without previous SSID/password command appears to reset wifi to factory settings
* command `100` is used for discovery over UDP broadcast (after device setup is complete)
    * this fails on some/most routers, which don't propagate UDP broadcasts
* command `109` disables wifi and creates a 'iKettle Update' network
    * a hard device reset (hold power button for 10 seconds) required to fix
