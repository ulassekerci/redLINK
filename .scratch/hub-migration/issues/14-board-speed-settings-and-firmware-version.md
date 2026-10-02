# Board speed settings and firmware version

Type: task
Status: resolved

Map: [Hub migration](../map.md)

## Question

[What the phone log contains](08-what-the-phone-log-contains.md) made the board's own settings the only source of speed and distance. Are those settings right, and does the board's firmware return every field the log expects?

A person with the car and VESC Tool does this and records the results here:

- Read wheel diameter, gear ratio and motor poles from the board's motor settings in VESC Tool and write down the three values.
- Check each against the car: measure the wheel, count the gear teeth, confirm the motor's pole count. Correct the board where it is wrong and note what changed. The old apps assumed 0.5 m, 5.88 and 22 poles, which disagrees with the board by about 7%.
- Record the board's firmware version. The odometer and uptime fields at the end of the `COMM_GET_VALUES_SETUP` reply exist in current firmware source; say whether this board's reply includes them.

## Answer

The board's firmware, 6.06, returns every field the log expects, and the speed settings do not hold up any decision. Resolved on 2026-10-02 without going to the car.

- **Speed settings:** the wheel diameter, gear ratio and pole count now on the board are rough estimates. The mechanics team will supply measured values and they will be entered in VESC Tool. No decision waits on the numbers: the design in [What the phone log contains](08-what-the-phone-log-contains.md) only requires that the board is the single source, so correcting it later needs no change in either app. Speed and distance in any log taken before the correction are off by whatever the estimates are off by.
- **When it must be done:** before the acceptance session. The specs carry it as a desk checklist item beside the others from [Testing without the car](16-testing-without-the-car.md): the three values on the board match the mechanics team's figures.
- **Firmware version:** 6.06, taken from the VESC Tool on the development Mac, which is 6.06 (the `version` key in its preferences and the newest entry of the changelog embedded in the binary) and is the same version as the board. It was not read from the board in this session.
- **Fields:** the `COMM_GET_VALUES_SETUP` handler is identical in the `release_6_05` and `release_6_06` tags of vedderb/bldc (`comm/commands.c`, lines 779 and 795). Both append the odometer as mask bit 20 and uptime in milliseconds as bit 21, each a `uint32`, as the last two fields. With the unmasked command (47) every field is sent, so the `odometer_m` and `board_uptime_ms` columns are filled on 6.06, and would be on 6.05 too.
- **Left to the Android spec:** the phone app can read the version with `COMM_FW_VERSION` on connect; whether it shows or logs it is not decided here.
