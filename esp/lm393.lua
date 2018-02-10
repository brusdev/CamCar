lm393 = {}

function lm393.create(pin, distance, threshold)

    local self = {}
    self.cb = nil
    self.count = 0
    self.time_last = 0
    self.speed = 0
    self.pin = pin or 1
    self.distance = distance or 10
    self.threshold = threshold or 3
    
    function self.pin_cb(level, when)
        self.count = self.count + 1
        if (self.count >= self.threshold) then
            self.count = 0
            self.speed = 3 * self.distance * 1000000 / (when - self.time_last)
            self.time_last = when
        end
    end

    function self.getSpeed()
        if tmr.now() - self.time_last > 1000000 then
            return 0
        else
            return self.speed
        end
    end
    
    gpio.mode(self.pin, gpio.INT)
    gpio.trig(self.pin, "up", self.pin_cb)
    
    return self
end