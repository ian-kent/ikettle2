package main

import (
	"bufio"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net"
	"strconv"
	"strings"
	"time"

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
	configureWifi         = 12
	configureWifiSSID     = 5
	configureWifiPassword = 7
	listWifiNetworks      = 13
	deviceInfo            = 100
	firmwareUpdate        = 109

	commandSentAck      = 3
	kettleOffBase       = 7
	kettleOnBase        = 8
	wifiNetworkResponse = 14
	deviceInfoResponse  = 101

	deviceKettle = 1
	deviceCoffee = 2
)

func main() {
	// create new shell.
	// by default, new shell includes 'exit', 'help' and 'clear' commands.
	shell := ishell.New()

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
		remoteConn, err = net.DialTimeout("tcp", settings.RemoteAddr+":2081", time.Duration(5)*time.Second)

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
					buf = []byte{}
				case autoDacKettleResponse:
					if len(buf) != 3 {
						shell.Println("invalid length for kettle dac response")
						buf = []byte{}
						continue
					}

					shell.Println("DAC response:", buf)
					buf = []byte{}
				case wifiNetworkResponse:
					// SSID,-db}SSID,-db}
					shell.Println("wifi response:", buf)
					s := string(buf)
					p := strings.Split(s, "}")
					for _, wn := range p {
						if len(wn) == 0 {
							buf = []byte{}
							continue
						}
						p2 := strings.SplitN(wn, ",", 2)
						shell.Println("=>", fmt.Sprintf("%s (%s dBm)", p2[0], p2[1]))
					}
					buf = []byte{}
				case deviceInfoResponse:
					if len(buf) != 3 {
						shell.Println("invalid length for device info response")
						buf = []byte{}
						continue
					}
					deviceType := int(buf[1])
					deviceSDK := int(buf[2])
					var deviceName string
					switch deviceType {
					case deviceKettle:
						deviceName = "iKettle 2.0"
					case deviceCoffee:
						deviceName = "Coffee"
					default:
						deviceName = "Unknown"
					}
					shell.Println("Device info:")
					shell.Println("=>", fmt.Sprintf("Type = %s [%d]", deviceName, deviceType))
					shell.Println("=>", fmt.Sprintf("SDK = %d", deviceSDK))
					buf = []byte{}
				case kettleStatus:
					if len(buf) != 6 {
						shell.Println("invalid length for kettle status update:", buf)
						shell.Println("`" + string(buf) + "`")
						buf = []byte{}
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
					buf = []byte{}
				default:
					shell.Println("Got command:", buf)
					shell.Println("ASCII: `" + string(buf) + "`")
					buf = []byte{}
				}
			}
		}()

		return "Connected to " + settings.RemoteAddr, nil
	})

	shell.Register("disconnect", func(args ...string) (string, error) {
		if remoteConn == nil {
			return "", errors.New("not connected")
		}

		err := remoteConn.Close()
		remoteConn = nil
		if err != nil {
			return "", err
		}

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

	shell.Register("cmd", func(args ...string) (string, error) {
		if remoteConn == nil {
			return "", errors.New("not connected")
		}

		var buf []byte
		for _, v := range args {
			i, e := strconv.Atoi(v)
			if e != nil {
				return "", fmt.Errorf("arg must be numeric byte value: %s", v)
			}
			buf = append(buf, byte(i))
		}

		if buf[len(buf)-1] != 126 {
			buf = append(buf, byte(126))
		}

		_, err := remoteConn.Write(buf)
		if err != nil {
			return "", err
		}

		return fmt.Sprintf("command sent: %v", buf), nil
	})

	shell.Register("wifi", func(args ...string) (string, error) {
		if remoteConn == nil {
			return "", errors.New("not connected")
		}

		_, err := remoteConn.Write([]byte{listWifiNetworks, endMessage})
		if err != nil {
			return "", err
		}

		return "wifi network list requested", nil
	})

	shell.Register("info", func(args ...string) (string, error) {
		if remoteConn == nil {
			return "", errors.New("not connected")
		}

		_, err := remoteConn.Write([]byte{deviceInfo, endMessage})
		if err != nil {
			return "", err
		}

		return "wifi network list requested", nil
	})

	shell.Register("setup", func(args ...string) (string, error) {
		if remoteConn == nil {
			return "", errors.New("not connected: switch to iKettle network, then `connect 192.168.4.1`")
		}

		shell.ShowPrompt(false)
		defer shell.ShowPrompt(true)

		shell.Print("Enter network SSID:")
		ssid := shell.ReadLine()

		shell.Print("Enter network password:")
		passwd := shell.ReadPassword()

		bSsid := []byte{configureWifiSSID}
		bSsid = append(bSsid, []byte(ssid)...)
		bSsid = append(bSsid, endMessage)
		shell.Println(fmt.Sprintf("Bytes: %v", bSsid))
		_, err := remoteConn.Write(bSsid)
		if err != nil {
			return "", fmt.Errorf("Error sending SSID command: %s", err)
		}

		bPasswd := []byte{configureWifiPassword}
		bPasswd = append(bPasswd, []byte(passwd)...)
		bPasswd = append(bPasswd, endMessage)
		_, err = remoteConn.Write(bPasswd)
		shell.Println(fmt.Sprintf("Bytes: %v", bPasswd))
		if err != nil {
			return "", fmt.Errorf("Error sending password command: %s", err)
		}

		bEnd := []byte{configureWifi, endMessage}
		shell.Println(fmt.Sprintf("Bytes: %v", bEnd))
		_, err = remoteConn.Write(bEnd)
		if err != nil {
			return "", fmt.Errorf("Error sending setup command: %s", err)
		}

		return "After ACK, switch to normal wifi network and `connect [kettle-ip]`", nil
	})

	// start shell
	shell.Start()
}
