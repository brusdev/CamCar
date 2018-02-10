hcsr04 = {}

function hcsr04.create(trigPin, echoPin, interval, callback)

    local self = {}
    self.distance = -1
    self.timer = nil
    self.time_echo = 0
    self.time_set = 0
    self.time_start = 0
    self.time_finish = 0
    self.time_measure = 0
    self.trig_pin = trigPin or 1
    self.echo_pin = echoPin or 2
    self.interval = interval or 0
    self.callback = callback or nil
    
    function self.echo_cb(level, when)
        if level == 1 then
            self.time_start = when
        else
            self.time_finish = when

            self.time_echo = self.time_finish - self.time_start;
            self.time_measure = self.time_finish - self.time_set;

            if self.time_echo < 30000 then
                self.distance = (self.time_echo) / 58
            end
            
            if self.callback ~= nil then
                self.callback(self.distance, self.time_echo, self.time_measure)
            end
        end
    end

    function self.measure()
        self.time_start = 0
        self.time_finish = 0
        self.time_set = tmr.now()

        gpio.write(self.trig_pin, gpio.HIGH)
        tmr.delay(10)
        gpio.write(self.trig_pin, gpio.LOW)
    end
    
    function self.getDistance()
        return self.distance
    end
    
    gpio.mode(self.trig_pin, gpio.OUTPUT)
    gpio.write(self.trig_pin, gpio.LOW)
    gpio.mode(self.echo_pin, gpio.INT)
    gpio.trig(self.echo_pin, "both", self.echo_cb)

    if self.interval > 0 then
        self.timer = tmr.create()
        self.timer:register(self.interval, tmr.ALARM_AUTO, self.measure)
        self.timer:start()
    end
    return self
end