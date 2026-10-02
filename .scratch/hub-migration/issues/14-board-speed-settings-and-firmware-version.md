# Board speed settings and firmware version

Type: task
Status: open

Map: [Hub migration](../map.md)

## Question

[What the phone log contains](08-what-the-phone-log-contains.md) made the board's own settings the only source of speed and distance. Are those settings right, and does the board's firmware return every field the log expects?

A person with the car and VESC Tool does this and records the results here:

- Read wheel diameter, gear ratio and motor poles from the board's motor settings in VESC Tool and write down the three values.
- Check each against the car: measure the wheel, count the gear teeth, confirm the motor's pole count. Correct the board where it is wrong and note what changed. The old apps assumed 0.5 m, 5.88 and 22 poles, which disagrees with the board by about 7%.
- Record the board's firmware version. The odometer and uptime fields at the end of the `COMM_GET_VALUES_SETUP` reply exist in current firmware source; say whether this board's reply includes them.
