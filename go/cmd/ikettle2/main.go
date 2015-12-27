package main

import (
	"bufio"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net"
	"strconv"

	"github.com/abiosoft/ishell"
)

var remoteConn net.Conn
var kettleState int
var pendingDAC bool

type settings struct {
	RemoteAddr    string
	OffBaseWeight int
}

const (
	autoDacKettle         = 44
	autoDacKettleResponse = 45
	endMessage            = 126
	kettleScheduleRequest = 65
	kettleStatus          = 20
	turnOffKettle         = 22
	turnOnKettle          = 21

	commandSentAck = 3
	kettleOffBase  = 7
	kettleOnBase   = 8
)

func main() {
	// create new shell.
	// by default, new shell includes 'exit', 'help' and 'clear' commands.
	shell := ishell.NewShell()

	// display welcome info.
	shell.Println("iKettle 2.0 shell")

	var settings settings
	var loadSettings = func() {
		b, err := ioutil.ReadFile("./.ikettle")
		if err == nil {
			err = json.Unmarshal(b, &settings)
			if err != nil {
				shell.Println("Error reading .ikettle settings file:", err)
			}
			return
		}
		shell.Println("Error reading .ikettle settings file:", err)
	}
	var saveSettings = func() {
		b, err := json.Marshal(&settings)
		if err != nil {
			shell.Println("Error saving .ikettle settings file:", err)
			return
		}
		err = ioutil.WriteFile("./.ikettle", b, 0660)
		if err != nil {
			shell.Println("Error saving .ikettle settings file:", err)
		}
	}
	loadSettings()

	// register a function for "connect" command.
	shell.Register("connect", func(args ...string) (string, error) {
		if remoteConn != nil {
			return "", errors.New("already connected - use 'disconnect' first")
		}

		if len(args) > 0 {
			settings.RemoteAddr = args[0]
			saveSettings()
		}

		if len(settings.RemoteAddr) == 0 {
			return "", errors.New("connect address missing, e.g. 'connect 192.168.0.10'")
		}

		var err error
		remoteConn, err = net.Dial("tcp", settings.RemoteAddr+":2081")

		if err != nil {
			remoteConn = nil
			return "", err
		}

		go func() {
			rdr := bufio.NewReader(remoteConn)
			var buf []byte
			for {
				b, err := rdr.ReadByte()
				if err != nil {
					shell.Println("Disconnected")
					if remoteConn != nil {
						go func(rc net.Conn) {
							defer func() {
								recover()
							}()
							rc.Close()
						}(remoteConn)
					}
					return
				}
				if b != endMessage {
					buf = append(buf, b)
					continue
				}

				switch buf[0] {
				case commandSentAck:
					shell.Println("OK!")
				case autoDacKettleResponse:
					if len(buf) != 3 {
						shell.Println("invalid length for kettle dac response")
						continue
					}

					shell.Println("DAC response:", buf)
				case kettleStatus:
					if len(buf) != 6 {
						shell.Println("invalid length for kettle status update")
						continue
					}

					flag := buf[1]
					var status string
					//var kettleReady, boilingInProgress, keepWarmInProgress, cycleFinished, babyCooling bool

					switch flag {
					case 0:
						status = "READY"
						//kettleReady = true
					case 1:
						status = "BOILING"
						//boilingInProgress = true
					case 2:
						status = "KEEP WARM"
						//keepWarmInProgress = true
					case 3:
						status = "FINISHED"
						//cycleFinished = true
					case 4:
						status = "COOLING"
						//babyCooling = true
					default:
						shell.Println("Invalid flag in kettle status:", flag)
					}

					temp := buf[2]
					waterlevel1 := int(buf[3])
					waterlevel2 := int(buf[4])

					wlevel := (waterlevel1 << 8) + waterlevel2
					wlevel2 := wlevel - settings.OffBaseWeight

					shell.Println("Status =>", status, fmt.Sprintf("(%d)", flag))
					shell.Println("Temperature =>", temp)

					var wlstatus string
					if wlevel2 < 130 {
						wlstatus = "EMPTY"
					} else if wlevel2 < 163 {
						wlstatus = "LOW"
					} else if wlevel2 < 203 {
						wlstatus = "HALF"
					} else if wlevel2 < 270 {
						wlstatus = "FULL"
					} else {
						wlstatus = "OVERFILLED"
					}

					shell.Println("Water level =>", wlevel, fmt.Sprintf("(%s)", wlstatus))

					if pendingDAC {
						settings.OffBaseWeight = int((waterlevel1 << 8) + waterlevel2)
						saveSettings()

						_, err := remoteConn.Write([]byte{autoDacKettle, endMessage})
						if err != nil {
							shell.Println("Error requesting AutoDAC:", err)
							return
						}

						pendingDAC = false
						shell.Println("Wait for DAC response")
					}
				default:
					shell.Println("Got command:", buf)
				}

				buf = []byte{}
			}
		}()

		return "Connected to " + settings.RemoteAddr, nil
	})

	shell.Register("disconnect", func(args ...string) (string, error) {
		if remoteConn == nil {
			return "", errors.New("not connected")
		}

		err := remoteConn.Close()
		if err != nil {
			return "", err
		}

		remoteConn = nil

		return "Disconnected from " + settings.RemoteAddr, nil
	})

	shell.Register("calibrate", func(args ...string) (string, error) {
		if remoteConn == nil {
			return "", errors.New("not connected")
		}

		shell.Println("Take kettle off base and press ENTER")
		_ = shell.ReadLine()
		pendingDAC = true

		return "Wait for status response", nil
	})

	shell.Register("on", func(args ...string) (string, error) {
		if remoteConn == nil {
			return "", errors.New("not connected")
		}

		temperature := 100

		if len(args) > 0 {
			tmp := args[0]
			v, err := strconv.Atoi(tmp)
			if err != nil {
				return "", errors.New("temperature must be valid number")
			}
			temperature = v
		}

		_, err := remoteConn.Write([]byte{turnOnKettle, byte(temperature), 0, endMessage})
		if err != nil {
			return "", err
		}

		return "switched on", nil
	})

	shell.Register("off", func(args ...string) (string, error) {
		if remoteConn == nil {
			return "", errors.New("not connected")
		}

		_, err := remoteConn.Write([]byte{turnOffKettle, endMessage})
		if err != nil {
			return "", err
		}

		return "switched off", nil
	})

	// 44 is setDac

	// start shell
	shell.Start()
}
