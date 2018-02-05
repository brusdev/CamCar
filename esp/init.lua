print("Init...")
tmr.alarm(0, 10000, tmr.ALARM_SINGLE, function() print("Load...") dofile("app.lua") end)
