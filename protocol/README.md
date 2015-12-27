Smarter iKettle 2.0 protocol
============================

Smarter iKettle 2.0 uses a binary protocol, either via UDP or TCP on port 2081.

Messages (commands and responses) use the syntax: `<id> <args> <terminator>`

The terminator is always byte `126`, or `~` in ASCII.

Arguments are listed as single bytes, using this syntax:

* `<arg>` is mandatory
* `<[arg]>` is optional

### Commands

| Command | Description            | Args
| ------- | ---------------------- | ----
| 21      | Turn on kettle         | `<[temp]> <[unknown]>`
| 22      | Turn off kettle        |
| 44      | Calibrate water sensor |
| 65      | Schedule request       | (unknown)

### Responses

| Response | Description                  | Args
| -------- | ---------------------------- | ----
| 3        | Command acknowledged         |
| 20       | Kettle status                | `<flag> <temp> <wlevel1><wlevel2> <unknown>`
| 45       | Calibrate completed response | `<unknown> <unknown>`

#### Useful info

`<wlevel1>` seems to be:

* `7` when the kettle is **off** the base
* `8` when the kettle is **on** the base
