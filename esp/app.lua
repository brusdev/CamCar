timeout = 0

function load()

print("Init gpio")

--Init led gpio
gpio.mode(0,gpio.OUTPUT);
gpio.write(0,gpio.LOW);

--Init pwm gpio
gpio.mode(1,gpio.OUTPUT);gpio.write(1,gpio.LOW);
gpio.mode(2,gpio.OUTPUT);gpio.write(2,gpio.LOW);

--Init sign gpio
gpio.mode(3,gpio.OUTPUT);gpio.write(3,gpio.HIGH);
gpio.mode(4,gpio.OUTPUT);gpio.write(4,gpio.HIGH);

pwm.setup(1,1000,1023);
pwm.start(1);pwm.setduty(1,0);
pwm.setup(2,1000,1023);
pwm.start(2);pwm.setduty(2,0);

print("Init wifi")

--Inti wifi.
wifi.setmode(wifi.STATION)

station_cfg={}
station_cfg.ssid="BrusNet"
station_cfg.pwd="dommiccargiafra"
wifi.sta.config(station_cfg)

station_ip={}
station_ip.ip = "192.168.10.9"
station_ip.netmask = "255.255.255.0"
station_ip.gateway = "192.168.10.1"
wifi.sta.setip(station_ip)

print("Init srv")

--Init udp server
srv=net.createServer(net.UDP)
srv:on("receive", onSrvReceive)
srv:listen(309)

tmr.alarm(1, 1000, tmr.ALARM_AUTO, resetTimer)

end

function resetTimer(timer)
    if (tmr.now() > timeout) then
        pwm.setduty(1, 0)
        pwm.setduty(2, 0)
    end
end

function onSrvReceive(srv, data, port, ip)
    print(ip .. ":" .. port .. ">" .. data)
    
    cmd = string.sub(data,1,3)
    arg = string.sub(data,5)
    print(cmd .. ":" .. arg)

    if cmd=="SET" then
        --OUT:0,0,0,0

        timeout = tmr.now() + 1000000;
        
        idx = 1
        sep = string.find(arg, ",", idx)
        lpwm = tonumber(string.sub(arg, idx, sep - 1))

        idx = sep + 1
        sep = string.find(arg, ",", idx)
        rpwm = tonumber(string.sub(arg, idx, sep - 1))

        idx = sep + 1
        sep = string.find(arg, ",", idx)
        ldir = tonumber(string.sub(arg, idx, sep - 1))

        idx = sep + 1
        rdir = tonumber(string.sub(arg, idx))

        pwm.setduty(1, 0)
        pwm.setduty(2, 0)

        gpio.write(3, ldir)
        gpio.write(4, rdir)

        pwm.setduty(1, lpwm)
        pwm.setduty(2, rpwm)

        srv:send(port, ip, "STA,1")
    elseif cmd=="OUT" then
        --OUT:3,0
        pin = tonumber(string.sub(arg, 1, 1))
        level = tonumber(string.sub(arg, 3))
        gpio.write(pin, level)
    elseif cmd=="PWM" then
        --PWM:1,1023
        pin = tonumber(string.sub(arg, 1, 1))
        level = tonumber(string.sub(arg, 3))
        pwm.setduty(pin, level)
    else
        print("Unknown command.")
    end
end

load()
